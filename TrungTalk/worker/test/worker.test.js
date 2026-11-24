import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

const env = { GEMINI_API_KEY: "test-key" };
const post = (path, payload) => new Request(`https://trungtalk.test${path}`, {
  method: "POST",
  headers: { "content-type": "application/json", "x-device-id": "worker-test" },
  body: JSON.stringify(payload)
});
const geminiResponse = text => new Response(JSON.stringify({
  candidates: [{ content: { parts: [{ text }] } }]
}), { status: 200, headers: { "content-type": "application/json" } });

test("health reports an unconfigured worker without exposing secrets", async () => {
  const response = await worker.fetch(new Request("https://trungtalk.test/health"), {});
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.ok, true);
  assert.equal(body.configured, false);
  assert.equal("GEMINI_API_KEY" in body, false);
});

test("missing API key is reported as a server configuration error", async () => {
  const response = await worker.fetch(post("/chat", { message: "你好" }), {});
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "server_not_configured");
});

test("invalid JSON is rejected before calling Gemini", async () => {
  const request = new Request("https://trungtalk.test/chat", {
    method: "POST", headers: { "content-type": "application/json" }, body: "{broken"
  });
  const response = await worker.fetch(request, env);
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "invalid_json");
});

test("translation retries an incomplete JSON batch and returns every Vietnamese gloss", async t => {
  const models = [];
  t.mock.method(globalThis, "fetch", async (url, init) => {
    const model = new URL(String(url)).pathname.split("/models/")[1].split(":")[0];
    models.push(model);
    const requestBody = JSON.parse(init.body);
    assert.equal(requestBody.generationConfig.responseMimeType, "application/json");
    assert.equal(requestBody.generationConfig.maxOutputTokens, 4096);
    const text = models.length === 1
      ? JSON.stringify([{ hanzi: "工作", meaning_vi: "công việc" }])
      : JSON.stringify([
          { hanzi: "工作", meaning_vi: "công việc; làm việc" },
          { hanzi: "设备", meaning_vi: "thiết bị" }
        ]);
    return geminiResponse(text);
  });
  const response = await worker.fetch(post("/translate", { words: [
    { hanzi: "工作", pinyin: "gōng zuò", english: "work" },
    { hanzi: "设备", pinyin: "shè bèi", english: "equipment" }
  ] }), env);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.model, "gemini-3.5-flash-lite");
  assert.deepEqual(models, ["gemini-3.8-flash", "gemini-3.5-flash-lite"]);
  const translated = JSON.parse(body.text);
  assert.equal(translated.length, 2);
  assert.equal(translated[1].meaning_vi, "thiết bị");
});

test("flashcard endpoint requires the requested count and Vietnamese fields", async t => {
  t.mock.method(globalThis, "fetch", async (_url, init) => {
    const requestBody = JSON.parse(init.body);
    assert.equal(requestBody.generationConfig.maxOutputTokens, 4096);
    const cards = Array.from({ length: 5 }, (_, i) => ({
      hanzi: `词${i + 1}`, pinyin: "cí", meaning_vi: "từ vựng",
      example: "这是一个词。", example_pinyin: "zhè shì yí ge cí", example_vi: "Đây là một từ."
    }));
    return geminiResponse(JSON.stringify(cards));
  });
  const response = await worker.fetch(post("/flashcard", { topic: "công việc", level: "HSK 2", count: 5 }), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).model, "gemini-3.8-flash");
});

test("evaluate endpoint only accepts a scored correction object", async t => {
  t.mock.method(globalThis, "fetch", async (_url, init) => {
    const requestBody = JSON.parse(init.body);
    assert.equal(requestBody.generationConfig.maxOutputTokens, 1800);
    return geminiResponse(JSON.stringify({
      score: 85, corrected: "我今天完成了工作。", explanation_vi: "Câu đúng ngữ pháp.",
      better_answer: "今天的工作已经完成了。", key_points: ["Dùng 已经 để nhấn mạnh hoàn thành"]
    }));
  });
  const response = await worker.fetch(post("/evaluate", { prompt: "Evaluate this sentence" }), env);
  assert.equal(response.status, 200);
  assert.equal(JSON.parse((await response.json()).text).score, 85);
});

test("grammar endpoint requires its explanation and examples structure", async t => {
  t.mock.method(globalThis, "fetch", async () => geminiResponse(JSON.stringify({
    explanation: "Cấu trúc câu đúng.", examples: ["我在工作。"], corrections: []
  })));
  const response = await worker.fetch(post("/grammar", { prompt: "解释 工作" }), env);
  assert.equal(response.status, 200);
  assert.equal(response.status, 200);
});

test("chat remains plain text and keeps the latest history window", async t => {
  let prompt = "";
  t.mock.method(globalThis, "fetch", async (_url, init) => {
    const requestBody = JSON.parse(init.body);
    prompt = requestBody.contents[0].parts[0].text;
    assert.equal(requestBody.generationConfig.responseMimeType, undefined);
    return geminiResponse("可以，我们继续练习。");
  });
  const history = Array.from({ length: 14 }, (_, i) => ({ role: "user", text: `turn-${i}` }));
  const response = await worker.fetch(post("/chat", { message: "继续", history }), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).text, "可以，我们继续练习。");
  assert.equal(prompt.includes("turn-0"), false);
  assert.equal(prompt.includes("turn-13"), true);
});
