V5.2 SMART LEARNING PLATFORM

Deploy EdgeOne:
1. Upload/deploy index.html from this ZIP as site root.
2. index.html already contains V5.1 Grade5 Smart Course + V5.2 Smart Platform inline.
3. Purge EdgeOne cache after deployment.
4. Browser hard reload Ctrl+Shift+R.

Included standalone patch:
- smart-platform-v5.2.js
This can be appended to a newer compatible source if the live site has changes newer than this package.

Important HSK note:
- Full HSK vocabulary is lazy-loaded from public raw GitHub data and cached in IndexedDB.
- If that dataset endpoint is unavailable, the existing built-in HSK core remains as fallback.

V5.3: Solo classroom compact (3 seats), math board portrait/landscape layout, canvas preservation on resize, reduced UI polling. Deploy the updated index.html and smart-platform-v5.2.js together as supplied in this ZIP.