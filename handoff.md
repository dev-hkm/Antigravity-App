# Handoff: Effort Picker Khong Hoat Dong Tren Mobile

## 2026-09-17: v1.3.9 — findings superseding the historical hypotheses below

- Confirmed architecture: the Angular host at `antigravity.google.com` loads remote control in a cross-origin iframe titled `Antigravity Remote Control`, from `https://antigravity.static.usercontent.goog/web/<version>/?iframe=true`. The host route module is `PGmR6b`.
- Examined public upstream bundle `https://antigravity.static.usercontent.goog/web/1.0.20260910010023/main.js`. The actual picker uses React + Base UI, NOT Radix. In `pIb`, desktop grouped-model `CQ.onClick` selects the default effort; `vIb` then closes the picker. Hover/keyboard opens the submenu. The mobile branch is selected by the `mobileLayout` feature flag, not simply the user agent.
- Primary integration bug: every previous `evaluateJavascript` listener ran in the TOP document. Touch events inside a cross-origin iframe never bubble to it. This explains the absence of any highlight/delay across prior patches.
- Additional v1.3.8 flaw: moving that script to the correct iframe still swallowed interactions; installing it at document start also threw because `document.documentElement` did not exist yet.
- v1.3.9 registers `WebViewCompat.addDocumentStartJavaScript` BEFORE navigation, restricted to the exact remote-control origin. Unsupported WebViews show an update notice. The new bridge does not require DOM readiness, private React internals, broad menu heuristics, or a timer swallowing all clicks.
- Only touch/pen clicks on actual desktop effort-group submenu triggers are intercepted. Focus + ArrowRight (ArrowLeft in RTL) uses the menu's existing keyboard behavior to open the submenu without selecting the default. Effort rows, ordinary models, native mobile buttons, mouse, and keyboard are not intercepted.
- Regression harness: `tools/test-effort-picker.cjs` loads the real public upstream Base UI and `CQ` components into a local intercepted cross-origin fixture; all other requests are blocked. No account, workspace, or API is accessed. It mirrors the upstream default-selection callback. This is a component-level browser reproduction, not a logged-in Android device test.
- Commands: set `PLAYWRIGHT_MODULE` to an installed Playwright module if needed; run `node tools/test-effort-picker.cjs parent` (reproduces default selection with parent-only injection), then `node tools/test-effort-picker.cjs frame` (touch High, repeated Low/Medium/High, whitespace taps, ordinary model, mouse, keyboard, hover; no JS errors).
- Removed old Kotlin string-presence tests: they asserted implementation strings, not whether users could select effort. `testReleaseUnitTest` is not configured in this project; use the browser regression harness and Release build/lint. No Debug APK required.
- Device acceptance remains pending: no attached ADB device. Install v1.3.9, reopen a remote session, tap a grouped model, then Low/Medium/High. Do not present browser/build success as confirmed device success.

## Historical notes (outdated implementation, retained for context)

## 1. Van de (user-facing)

Tren Antigravity Android app, khi mo model picker (nhan nut chon model o thanh input), dropdown danh sach model hien ra. Khi cham vao mot model bat ky, model duoc chon ngay va dropdown dong lai luon - user KHONG BAO GIO thay duoc submenu effort (Low / Medium / High / Auto) nen khong the chon effort level.

Tren desktop browser flow dung phai la: hover chuot vao mot model -> submenu effort tu hien ben canh -> click muc effort -> model + effort deu duoc chot roi menu moi dong.

Tren man hinh cam ung (Android WebView) khong co hover, nen submenu effort khong bao gio xuat hien. Moi lan cham = chon model luon + dong menu.

## 2. Context ky thuat

- App la Android WebView wrapper cho https://antigravity.google.com (Kotlin + Jetpack Compose, applicationId com.hkm.antigravity).
- Trang web duoc cho la dung Radix UI cho dropdown/menu (nhan dien qua [data-radix-popper-content-wrapper], [role=menuitem]...). CHUA KIEM CHUNG - co the sai, web co the dung custom menu / Shadow DOM / iframe.
- File duy nhat lien quan: app/src/main/java/com/hkm/antigravity/ui/components/WebViewContainer.kt, ham injectWebTheme() - inject JS + CSS vao WebView qua evaluateJavascript o onPageStarted / onPageFinished.
- Flag guard window.__antiMobileSetup dam bao listener chi gan 1 lan.
- Hien tai version trong app/build.gradle.kts la 1.3.7 (versionCode 13). APK signed cuoi: app/build/outputs/apk/release/Antigravity-v1.3.7-two-tap-effort-picker.apk.
- LUU Y QUAN TRONG CHO AI TIEP THEO: user test tren may that, moi ban v1.3.1 -> v1.3.7 deu bi user bao van vay luon - nghia la KHONG co chuyen bien nao quan sat duoc (khong highlight, khong delay, khong submenu). Hay nghi ngo ca kha nang: (a) script khong chay toi doan listener (loi JS silent o phia tren lam ca IIFE chet?), (b) selector [data-radix-popper-content-wrapper] / [role=menuitem] khong khop DOM that cua antigravity.google.com, (c) menu dong boi logic noi bo khac. Buoc dau tien nen la them remote debugging / console-log capture (setWebContentsDebuggingEnabled + chrome://inspect, hoac WebChromeClient.onConsoleMessage ra logcat) de nhin DOM that va event flow, thay vi doan selector tiep.

## 3. Lich su cac lan fix da thu (v1.3.1 -> v1.3.7, TAT CA FAIL)

- v1.3.1: fix mau switch, pill drop positioning, unblock model/effort dropdown (mo dau chuoi effort).
- v1.3.2: fix web theme override, unblock effort picker hover, precise pill drop, crisp switches.
- v1.3.3: resolve web theme capture, dynamic color override, pill coordinate bounce, radix menu interaction.
- v1.3.4: rewrite sach WebViewContainer, dynamic color qua colorScheme.primary moi noi, touchend effort-blocker.
- v1.3.5: smarter effort-picker - dem so menu layer truoc/sau hover de quyet dinh co block touch->click khong.
- v1.3.6 (commit 53c437b): intercept pointerdown capture phase - block model tap khi dung 1 popper mo, pass-through khi 2+ poppers, dispatch fake pointerover/pointerenter/mouseenter/mouseover/mousemove, fallback click() sau 400ms.
- v1.3.7 (commit 58ef33f, ban moi nhat): gia thuyet root cause la (1) fake hover la synthetic isTrusted=false nen React/Radix ignore, (2) preventDefault() o pointerdown khong chan duoc compatibility click sau pointerup. Fix = co che TWO-TAP: chan ca pointerdown + touchend + click o capture phase khi dung 1 popper mo, highlight vien xanh item dang arm (outline: 2px solid #3b82f6), ban hover chain + focus(), fallback el.click() sau 450ms neu khong co submenu; cham lan 2 vao cung item trong 3s (hoac khi 2+ poppers / 0 popper) thi pass-through. Selector mo rong them [role=menuitemradio], [data-radix-collection-item]. -> User van bao van vay luon, ke ca highlight xanh cung khong thay.

## 4. Code hien tai cua interceptor (de AI tiep theo doc nhanh)

Nam trong injectWebTheme(), sau phan CSS override (xem WebViewContainer.kt dong ~118-215):

  if (!window.__antiMobileSetup) {
      window.__antiMobileSetup = true;
      function radixPopperCount() { return document.querySelectorAll(...data-radix-popper-content-wrapper...).length; }
      function closestMenuItem(n) { return n.closest(...menuitem / menuitemradio / option / data-radix-collection-item...); }
      // state: __antiArmedItem, __antiArmedTime, __antiFallbackTimer
      // disarm() / armItem(item) / shouldBlockItem(item)
      // pointerdown + touchend (capture) -> preventDefault + stopPropagation + stopImmediatePropagation + armItem
      // click (capture) -> swallow neu item dang armed trong 800ms
  }

## 5. Goi y huong dieu tra cho AI tiep theo (chua lam)

1. Bat WebView.setWebContentsDebuggingEnabled(true) o ban debug, dung chrome://inspect xem DOM that cua model picker: co dung la Radix khong, selector nao khop, menu nam o document / shadow root / iframe?
2. Gan WebChromeClient.onConsoleMessage log ve logcat de thay JS co chay, radixPopperCount() tra ve may khi menu mo.
3. Native khong intercept touch gi ca (chi co WebView mac dinh + BackHandler), nen kha nang cao van de nam o JS/DOM.
4. Can nhac phuong an khong chan event nua: tim trigger cua submenu effort trong React fiber / __reactProps va goi truc tiep, hoac long-press (giu 500ms) thay vi two-tap.
5. May ban dau dau co bi dau (theo user o prompt 3) - co the web antigravity.google.com tu doi UI (truoc day effort la radio group rieng, gio thanh hover-submenu). Check lai voi ban web hien tai.

## 6. 6 Prompt gan nhat cua user

1. van khong duoc!! van con mot so cho khong co dynamic color, roi may cai kia van y chang, khong he fix duoc!!!
2. Ok thi gio con dung 1 loi thoi, do la chon model no van khong cho giu de chon effort!
3. van khong duoc! lan nay ban rang tim ra root cause roi fix triet de xem! may ban dau dau co bi dau!!!!
4. van khong duoc! (sau khi AI release ban v1.3.6)
5. thoi gio ban viet nhanh file handoff.md vao workspace di, mieu ta chi tiet loi nay vao, nhet 5 prompt gan nhat cua toi vao luon, de toi keu AI khac vao xu ly
6. van vay luon! thoi ban update file handoff di, de toi keu AI khac fix (sau khi AI release ban v1.3.7 two-tap - user xac nhan ke ca highlight xanh cung khong xuat hien)
