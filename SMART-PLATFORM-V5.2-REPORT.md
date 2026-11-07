# LỚP HỌC THÔNG MINH AI — V5.2 SMART LEARNING PLATFORM

## 1. Grade 5
- Toán 5: giữ Smart Course V5.1, 32 bài + HK1/HK2, tự chuyển bài, không reset vòng lặp.
- Tiếng Việt 5: giữ Smart Course V5.1, 31 bài + HK1/HK2, tự chuyển bài, không reset vòng lặp.
- Tiếng Anh 5: 20 Units × 30 activity = 600 hoạt động.
- Tiếng Anh lớp 5 HK1/HK2: 40 câu/đề, lấy cân bằng theo Unit.
- SRS ghi seen/correct/wrong/mastery/due cho từng activity.

## 2. CEFR A1–C1
- A1: 600 hoạt động
- A2: 700 hoạt động
- B1: 800 hoạt động
- B2: 900 hoạt động
- C1: 900 hoạt động
- Tổng: 3,900 hoạt động
- 20 Unit mỗi level.
- Hoạt động: Vocabulary, Listening, Speaking, Dialogue, Grammar, Reading, Task, Interaction, Mediation, Review.
- Học tuần tự; hết level tự chuyển level tiếp theo, không shuffle lại để lặp vô hạn.
- Test Center: Checkpoint 20 câu, Progress 30 câu, Final 50 câu.

## 3. HSK 1–6
V5.2 dùng HSK 2.0 cho lộ trình 1–6 hiện tại (regular 2026), tải dữ liệu theo level từ complete-hsk-vocabulary.

Cumulative vocabulary:
- HSK1: 150 từ => 1,200 activity slots
- HSK2: 300 từ => 2,400 activity slots
- HSK3: 600 từ => 4,800 activity slots
- HSK4: 1,200 từ => 9,600 activity slots
- HSK5: 2,500 từ => 20,000 activity slots
- HSK6: 5,000 từ => 40,000 activity slots

8 activity types/word:
1. meaning
2. pinyin
3. hanzi
4. tone
5. listening
6. speaking
7. context
8. review

Implementation: virtual Proxy bank; không materialize hàng chục nghìn object cùng lúc. Dataset được cache IndexedDB theo level.

## 4. Practice Hub / SRS
State per activity:
- seen
- correct
- wrong
- mastery 0–5
- lastSeen
- due

Correct answer increases mastery and schedules 6h/24h/72h/168h/336h. Wrong answer lowers mastery and reschedules after 10 minutes.
Practice Hub receives Smart Review card with due/accuracy/mastered metrics and 20-question intelligent review.

## 5. Test Center
- Grade 5: HK1/HK2 per subject.
- CEFR: Checkpoint / Final by level.
- HSK: Mini 10 / Unit 25 / Mock level-based.
- Adaptive Test: 30 mixed questions, designed to prioritize weak/mastery data.
- Test history stored locally with score, total, percent and timestamp.

## 6. Anti-repeat rules
- Grade 5: lesson finishes => completed => next lesson.
- CEFR: sequential level bank => next CEFR level when complete.
- HSK: virtual index advances through full level; when complete => next HSK level.
- SRS repetition happens only when due/weak, not random immediate recycling.

## 7. Data / copyright
- Grade 5 and CEFR activities are original generated learning activities aligned to skills and curriculum structure; no textbook pages are copied.
- HSK vocabulary data source: drkameleon/complete-hsk-vocabulary (MIT); dictionary meanings derive from CC-CEDICT (CC BY-SA 4.0). Include attribution when redistributing the dataset cache as a bundled asset.

## 8. Standards grounding
- CEFR structure follows action-oriented, can-do communicative activities: reception, production, interaction, mediation.
- HSK 1–6 bank is configured for the current six-level HSK pathway; engine is isolated so a later HSK 3.0 dataset switch can be added without changing UI/test/SRS architecture.

## 9. Build validation
- All classic inline scripts syntax-check: PASS (importmap excluded).
- V5.2 standalone JS: node --check PASS.
- CEFR target counts: 600/700/800/900/900 = 3,900.
- Grade5 English target: 20 × 30 = 600.
- HSK virtual activity slots by level: 1,200 / 2,400 / 4,800 / 9,600 / 20,000 / 40,000.


## 10. V5.3 UI / performance hotfix
- Solo classroom renders only the learner and two AI classmates (3 seats total); multiplayer retains the full roster.
- Compact student cards with smaller avatars to reserve screen area for lesson content.
- Math writing board supports portrait and landscape layouts; the canvas gets the dominant area and the question/options panel is secondary.
- Canvas resizing/orientation changes preserve existing handwriting and cap device-pixel-ratio at 2 to reduce memory/paint cost.
- Reduced nonessential HSK visibility polling from 500 ms to 2.5 s and header label polling from 2 s to 10 s.
- Validation: all 13 classic inline JavaScript blocks pass `node --check`.