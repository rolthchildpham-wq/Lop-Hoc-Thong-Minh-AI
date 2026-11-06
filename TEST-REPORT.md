# TEST REPORT — Lớp Học Thông Minh AI v4.4

- Logic/data automated: **44/44 PASS**
- Deployed website surface checked at `https://aihuedanlearn.edgeone.dev/`: setup, CEFR A1-C1, Learning Hub, HSK 1-6, Grade 5 program entries, reconnect button, voice controls are present.
- JavaScript syntax checked for local scripts; `korva-voice.js` is an ES module and is intentionally excluded from classic-script parser checks.

## Bug fixed during audit
- Fixed legacy shortened `q(...)` records in Grade 5 Vietnamese V2-V8 where question/options/correct/explain fields were shifted. The data factory now normalizes both full and legacy call formats.

## Offline/online matrix
| Feature | Online | Offline bundled |
|---|---:|---:|
| CEFR A1-C1 | ✓ | ✓ |
| Toán 5 / Tiếng Việt 5 / Tiếng Anh 5 | ✓ | ✓ |
| HSK core + quiz | ✓ | ✓ |
| Learning Hub / XP / streak / local score | ✓ | ✓ |
| Firebase 1v1 / reconnect cloud | ✓ | — |
| KorvaTTS model download | ✓ | fallback system voice |
| Hanzi Writer new character data | ✓ | fallback static character |

## Automated checks
- PASS — grade5_math: dữ liệu hợp lệ — total=96, bad=0, duplicateQuestions=0
- PASS — grade5_math: ngân hàng >= 90 — total=96
- PASS — grade5_viet: dữ liệu hợp lệ — total=96, bad=0, duplicateQuestions=7
- PASS — grade5_viet: ngân hàng >= 90 — total=96
- PASS — grade5_eng: dữ liệu hợp lệ — total=120, bad=0, duplicateQuestions=1
- PASS — grade5_eng: ngân hàng >= 90 — total=120
- PASS — CEFR A1: dữ liệu hợp lệ — count=88, bad=0, kinds={"vocab":64,"mcq":24}
- PASS — CEFR A1: >= 80 hoạt động — count=88
- PASS — CEFR A2: dữ liệu hợp lệ — count=88, bad=0, kinds={"vocab":64,"mcq":24}
- PASS — CEFR A2: >= 80 hoạt động — count=88
- PASS — CEFR B1: dữ liệu hợp lệ — count=88, bad=0, kinds={"vocab":64,"mcq":24}
- PASS — CEFR B1: >= 80 hoạt động — count=88
- PASS — CEFR B2: dữ liệu hợp lệ — count=88, bad=0, kinds={"vocab":64,"mcq":24}
- PASS — CEFR B2: >= 80 hoạt động — count=88
- PASS — CEFR C1: dữ liệu hợp lệ — count=88, bad=0, kinds={"vocab":64,"mcq":24}
- PASS — CEFR C1: >= 80 hoạt động — count=88
- PASS — HSK 1: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 1: >=60 — count=75
- PASS — HSK 2: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 2: >=60 — count=75
- PASS — HSK 3: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 3: >=60 — count=75
- PASS — HSK 4: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 4: >=60 — count=75
- PASS — HSK 5: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 5: >=60 — count=75
- PASS — HSK 6: dữ liệu hợp lệ — count=75, bad=0
- PASS — HSK 6: >=60 — count=75
- PASS — HSK tổng >=400 — total=450
- PASS — Syntax app.js 
- PASS — Syntax audit-logic.js 
- PASS — Syntax cefr-english-bank.js 
- PASS — Syntax firebase-config.js 
- PASS — Syntax firebase-multiplayer.js 
- PASS — Syntax grade5-bank.js 
- PASS — Syntax hsk-pro-data.js 
- PASS — Syntax hsk-pro.js 
- PASS — Syntax learning-hub.js 
- PASS — Syntax state-smoke-test.js 
- PASS — Syntax voice-manager.js 
- PASS — State normalizeLearningSub tồn tại 
- PASS — Firebase switchTopic render UI 
- PASS — Firebase mode gọi setAppMode 
- PASS — Không còn default CEFR comm trong Firebase 