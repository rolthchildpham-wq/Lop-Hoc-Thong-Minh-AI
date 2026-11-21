# Chinese Learn — Guided Courses

This directory integrates the user's supplied `chinese-learn.zip` into the existing **Lớp Học Thông Minh AI** website without replacing its root application.

- Entry point: `/chinese-learn/` (`chinese-learn/index.html`)
- Original CodePen: https://codepen.io/Bacpham/pen/ogWbRxy
- License: MIT (see `LICENSE.txt`)
- Languages: Vietnamese, Chinese, English. Six course-level cards, lessons/stories counts, illustrative progress indicators, preference controls.

The source HTML shows a guided-course landing interface. It does **not** include actual lesson/stories content or a backend. Progress bars in the original are fixed at 0%; do not claim full course data is bundled.

## EdgeOne deployment

Deploy the whole repository's static site so `chinese-learn/index.html` is preserved. Verify `https://aihuedanlearn.edgeone.dev/chinese-learn/` in Preview before releasing Production. The main site's root `index.html` remains unchanged by this branch.

**Important**: importing a subpage does not yet add a launcher to the main app's navigation or sync its learning progress with the root HSK system. Those require integration changes to the 1MB root HTML and explicit QA. Do not present this branch as a full live-site rollout.
