# TrungTalk Android 2.0

Native Kotlin + Jetpack Compose Android app based on the supplied Trungtalk.docx.

## Integrated in this build
- 12-month curriculum in 5 phases, lesson completion persistence and Mandarin TTS normal/slow.
- Material 3 light/dark theme toggle and expanded navigation.
- Room database foundation for grammar history, SRS/Leitner cards and practice logs.
- Grammar practice UI with local heuristic feedback (not AI grading).
- Five work roleplay scenario prompts.
- HSK sample quiz, XP/level summary and SRS due-card screen.
- Android zh-CN speech recognition. It returns a transcript, not phoneme-level pronunciation scoring.

## Requires owner configuration
Gemini chat, AI grammar and AI roleplay require a deployed Cloudflare Worker and a server-side Gemini key. Azure Speech phoneme scoring requires an Azure Speech subscription. Whisper requires an OpenAI API key. Offline Gemma requires a separately downloaded model (~1.5 GB), a compatible device and model license acceptance. Wear OS needs a paired watch and separate Wear app module. These services are not activated until credentials/devices are supplied and tested.

### Deploy Worker
1. Install Wrangler: `npm install -g wrangler`
2. `cd TrungTalk/worker && wrangler login`
3. `wrangler secret put GEMINI_API_KEY` (enter your own key in terminal)
4. `wrangler deploy`
5. Build with `-PBACKEND_URL=https://YOUR-WORKER.workers.dev`

Do not put API keys in source code or chat.

## Build
`gradle -p TrungTalk :app:assembleDebug`
`gradle -p TrungTalk :app:assembleRelease`

For a signed production APK, configure GitHub Actions secrets: RELEASE_KEYSTORE_BASE64, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD. Use one permanent keystore and preserve it for future updates.

## Deploy Cloudflare AI Worker and build a connected APK

The repository includes `.github/workflows/trungtalk-cloudflare-deploy.yml`. It deploys `TrungTalk/worker` to Cloudflare Workers, stores the Gemini key as a Worker secret, checks `/health`, then builds an installable Debug APK with the Worker URL compiled into `BuildConfig.BACKEND_URL`.

Before running the workflow, configure these GitHub repository settings:

**Actions secrets**
- `CLOUDFLARE_API_TOKEN`: Cloudflare API token with permission to edit Workers Scripts for the target account.
- `GEMINI_API_KEY`: Gemini API key. Keep this secret; never commit it or paste it into chat.

**Actions variable**
- `CLOUDFLARE_ACCOUNT_ID`: Account ID shown in the Cloudflare dashboard.

Then open GitHub → Actions → **Deploy TrungTalk AI Worker and Build APK** → Run workflow. The run summary prints the Worker URL and health-check URL. The APK is published as artifact `TrungTalk-Android-AI-DEBUG-APK`.

The Worker is named `trungtalk-proxy` and uses the `workers.dev` subdomain. Enable workers.dev for the account if deployment reports that the subdomain is unavailable. The Gemini key is installed with `wrangler secret put`; it is not embedded in Android.

This workflow requires the repository owner to add the three settings above. ChatGPT cannot read or create Cloudflare/GitHub secrets on the owner's behalf.
