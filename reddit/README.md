# Upvote Saver (reddit/)

A small Android app for personal use: it signs in to your own Reddit account and saves the images,
GIFs and videos of the posts you upvote to the phone's gallery. It is a separate app from the game
in this repository (its own `applicationId`, `com.upvotesaver`) and shares only the Gradle setup.

Only Reddit's official OAuth API is used, with your own app registration. Follow Reddit's API terms;
the app is meant for one person's own upvotes.

## Redgifs links (no account needed)

Paste a Redgifs link into the **Redgifs link** card, or use **Share** in another app and pick Upvote Saver.
The clip's HD file (with sound) is saved as `redgifs_<poster>_<id>.mp4` in `Movies/UpvoteSaver`.
Page links (`redgifs.com/watch/<id>`, `/ifr/<id>`) and direct file links are recognised; several links in one text all download.
A Reddit post link cannot be used: reading a Reddit post needs the API access described below.
Redgifs ties its temporary token to the device's network address and User-Agent, so the lookup and the download must come from the same connection.

## Redgifs account and niches

- **Sign in to Redgifs** opens the Redgifs site in an in-app browser. Sign in with email/user name and password
  (Google sign-in is refused inside apps). The app never sees the password: it notes the bearer token the site
  itself sends to `api.redgifs.com` and keeps it only if `GET /v1/me` accepts it.
- **Check for new likes / Download all my likes** read `GET /v2/feeds/liked` and save each clip once.
  The automatic schedule checks likes too.
- The token expires. While the app is open the site is loaded out of sight so a refreshed token is picked up;
  in the background a rejected token stops the likes check until the app is opened (or you sign in again).
- **Browse niches**: search niches (`GET /v2/niches/search`; blank = most followed), open one, sort its clips
  (most liked, hot, trending, newest: `GET /v2/niches/{id}/gifs`), tick the ones you want and download.

These routes are the ones the Redgifs site uses; they are not publicly documented and may change.

## Setup for Reddit upvotes (once)

Since November 2025 Reddit requires approval before a new app may use its API (Responsible Builder Policy);
until the request is approved, sign-in does not work.

1. On the phone open <https://www.reddit.com/prefs/apps> and choose **create another app**.
2. Type: **installed app**. Name: anything. Redirect URI: `upvotesaver://auth`.
3. Copy the ID shown under the app's name into the app's **App ID** field and tap **Sign in with Reddit**.
4. Approve the access in the browser; it returns to the app.

Reddit may ask new API users to request access before an app registration works.
The app asks for the scopes `identity` (your user name), `history` (your upvoted list) and `read`.

## Use

- **Check for new upvotes**: reads your upvoted list from the newest post and stops at the first page with nothing new.
- **Scan all past upvotes**: reads every page Reddit offers. Reddit lists about the last 1,000 upvotes.
- **Import post_votes.csv**: for older upvotes. Request your data at <https://www.reddit.com/settings/data-request>,
  unpack the archive and pick `post_votes.csv`; the app downloads every upvoted post in it.
- **Automatic download**: a background check every 15 minutes to once a day (Android may delay it), optionally only on Wi-Fi.

Files go to `Pictures/UpvoteSaver` and `Movies/UpvoteSaver`, named `subreddit_postid_title`.
With **Hide from gallery** on they go to `Download/UpvoteSaver` instead, which holds a `.nomedia` marker so gallery
apps skip it; files saved earlier are moved there (and moved back when the switch is turned off).
Each post is handled once; a failed download is retried on the next runs, three times in total.

## What is saved

| Post | How |
| --- | --- |
| Reddit video (`v.redd.it`) | Best picture and sound streams from the DASH manifest, joined into one MP4 on the phone (no re-encoding). |
| Reddit image / GIF (`i.redd.it`) | The file as is. |
| Gallery | Every item, numbered `_1`, `_2`, … |
| Crosspost | The original post's media. |
| Imgur direct links, `.gifv` | The file (`.gifv` as MP4). |
| Redgifs | The HD file (with sound) through Redgifs' API; if that fails, Reddit's silent preview. |
| Other hosts | Reddit's own copy when it has one: a silent preview MP4 or the full-size preview image. |

Text posts, plain links, and hosts without a Reddit copy (for example YouTube) are marked "nothing to save".

## Layout

| Module | Contents |
| --- | --- |
| `reddit/core` | Pure Kotlin/JVM: `Http` (OkHttp, retries), `RedditAuth` + `Session` (OAuth, token refresh), `RedditApi`, `MediaResolver` + `DashStreams`, `Redgifs` + `RedgifsLinks` + `RedgifsSaver` (links, likes, niches), `VotesCsv`, `PostStore` (JSON-lines log), `SyncEngine`. |
| `reddit/app` | Compose UI, `MainViewModel`, `SyncWorker` (WorkManager: schedule and foreground runs), `RedgifsUi` (in-app sign-in, niche browser), `MediaStoreSink`, `AndroidMuxer` (MediaExtractor → MediaMuxer). English and Turkish strings. |

## Commands

```bash
./gradlew :reddit:core:test          # unit tests (MockWebServer)
./gradlew :reddit:app:assembleDebug  # APK: reddit/app/build/outputs/apk/debug/app-debug.apk
./gradlew :reddit:app:installDebug   # install on a connected phone
./gradlew :reddit:app:lintDebug
```
