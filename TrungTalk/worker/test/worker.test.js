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

test("flashcard output rejects duplicate Hanzi and retries a valid fallback batch", async t => {
  let calls = 0;
  t.mock.method(globalThis, "fetch", async () => {
    calls++;
    const cards = Array.from({ length: 5 }, (_, i) => ({
      hanzi: calls === 1 ? (i === 4 ? "词1" : `词${i + 1}`) : `新词${i + 1}`,
      pinyin: "cí", meaning_vi: "từ vựng",
      example: "这是一个词。", example_pinyin: "zhè shì yí ge cí", example_vi: "Đây là một từ."
    }));
    return geminiResponse(JSON.stringify(cards));
  });
  const response = await worker.fetch(post("/flashcard", { topic: "công việc", level: "HSK 2", count: 5 }), env);
  assert.equal(response.status, 200);
  assert.equal(calls, 2);
  const body = await response.json();
  assert.equal(body.model, "gemini-3.5-flash-lite");
  const cards = JSON.parse(body.text);
  assert.equal(new Set(cards.map(card => card.hanzi)).size, 5);
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


test("API aliases, preflight and unsupported methods have predictable responses", async t => {
  t.mock.method(globalThis, "fetch", async () => geminiResponse("你好，我们开始吧。"));
  const preflight = await worker.fetch(new Request("https://trungtalk.test/api/chat", { method: "OPTIONS" }), env);
  assert.equal(preflight.status, 204);
  assert.equal(preflight.headers.get("access-control-allow-origin"), "*");
  const unsupported = await worker.fetch(new Request("https://trungtalk.test/chat", { method: "GET" }), env);
  assert.equal(unsupported.status, 404);
  const alias = await worker.fetch(new Request("https://trungtalk.test/api/health"), {});
  assert.equal(alias.status, 200);
  assert.equal((await alias.json()).ok, true);
});

test("empty prompts are rejected without making an upstream request", async t => {
  t.mock.method(globalThis, "fetch", async () => {
    assert.fail("Gemini must not be called for an empty prompt");
  });
  const response = await worker.fetch(post("/chat", { message: "   " }), env);
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "empty_prompt");
});

test("invalid structured model output is rejected and falls back to the next model", async t => {
  const models = [];
  t.mock.method(globalThis, "fetch", async url => {
    const model = new URL(String(url)).pathname.split("/models/")[1].split(":")[0];
    models.push(model);
    return geminiResponse(models.length === 1
      ? JSON.stringify({ score: 140, corrected: "x", explanation_vi: "bad" })
      : JSON.stringify({ score: 90, corrected: "我很好。", explanation_vi: "Câu đúng." }));
  });
  const response = await worker.fetch(post("/evaluate", { prompt: "我很好" }), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).model, "gemini-3.5-flash-lite");
  assert.equal(models.length, 2);
});

test("rate limiting returns a retry hint after the configured device quota", async () => {
  let count = 0;
  const rateStore = {
    async get() { return String(count); },
    async put(_key, value) { count = Number(value); }
  };
  count = 25;
  const response = await worker.fetch(post("/chat", { message: "hello" }), { ...env, RATE_LIMIT: rateStore });
  assert.equal(response.status, 429);
  assert.equal((await response.json()).retry_after, 60);
});

test("translate rejects duplicate or missing source words in model output", async t => {
  t.mock.method(globalThis, "fetch", async () => geminiResponse(JSON.stringify([
    { hanzi: "工作", meaning_vi: "công việc" },
    { hanzi: "工作", meaning_vi: "làm việc" }
  ])));
  const response = await worker.fetch(post("/translate", { words: [
    { hanzi: "工作", english: "work" }, { hanzi: "设备", english: "equipment" }
  ] }), env);
  assert.equal(response.status, 502);
  assert.equal((await response.json()).error, "upstream_error");
});


test("translate rejects oversized batches and malformed word fields before calling Gemini", async t => {
  t.mock.method(globalThis, "fetch", async () => {
    assert.fail("Gemini must not be called for invalid translation input");
  });
  const tooMany = await worker.fetch(post("/translate", {
    words: Array.from({ length: 21 }, (_, i) => ({ hanzi: `词${i}` }))
  }), env);
  assert.equal(tooMany.status, 413);
  assert.equal((await tooMany.json()).error, "batch_too_large");

  const invalid = await worker.fetch(post("/translate", {
    words: [{ hanzi: "工作", pinyin: "p".repeat(101), english: "work" }]
  }), env);
  assert.equal(invalid.status, 400);
  assert.equal((await invalid.json()).error, "invalid_translation_word");
});
