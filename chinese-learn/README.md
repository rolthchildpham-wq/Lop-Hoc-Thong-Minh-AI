# Chinese Learn — Guided Courses

This directory integrates the user's supplied `chinese-learn.zip` into the existing **Lớp Học Thông Minh AI** website without replacing its root application.

- Entry point: `/chinese-learn/` (`chinese-learn/index.html`)
- Original CodePen: https://codepen.io/Bacpham/pen/ogWbRxy
- License: MIT (see `LICENSE.txt`)
- Languages: Vietnamese, Chinese, English. Six course-level cards, lessons/stories counts, illustrative progress indicators, preference controls.

The source HTML shows a guided-course landing interface. It does **not** include actual lesson/stories content or a backend. Progress bars in the original are fixed at 0%; do not claim full course data is bundled.

## EdgeOne deployment

Deploy the whole repository's static site so `chinese-learn/index.html` is preserved. Verify `https://aihuedanlearn.edgeone.dev/chinese-learn/` in Preview before releasing Production. The root `index.html` now includes Chinese Learn links in its setup modal and top navigation; existing classroom and HSK logic remains intact.

**Important**: the root navigation links are present, but learning progress is not synchronized with the root HSK system. The supplied archive contains a course landing UI, not a completed lesson engine. Browser QA and EdgeOne Preview are still required before production; do not present this branch as a fully tested live rollout.
