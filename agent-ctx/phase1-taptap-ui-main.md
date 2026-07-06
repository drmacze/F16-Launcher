# Task: F16-Launcher TapTap-Level UI/UX Phase 1 — Design System + Beranda Redesign

## Agent
- Agent name: code (main agent)
- Date: 2025-07-02
- Token push provided: yes

## Files Changed

### Created
1. `app/src/main/java/com/drmacze/f16launcher/TapTapDesignSystem.kt`
   - Single source of truth untuk design tokens (Carbon, GlassBase, Surface2,
     Surface3, CandyCyan, CandyBlue, NeonGreen, SoftText, SubText, GlassStroke,
     DangerRed, AmberWarn, TapTapGreen, TapTapGold)
   - TTTypography (displayLarge → micro)
   - TTSpacing (xs → xxxl)
   - TTShapes (card, cardLarge, button, chip, input, small)
   - TTElevation (card=0, modal=8, nav=12)

2. `app/src/main/java/com/drmacze/f16launcher/TapTapComponents.kt`
   - TTShimmerBox (valentinilk shimmer)
   - TTGameCardSkeleton (skeleton placeholder)
   - TTTappableCard (press scale spring bounce)
   - TTBannerCarousel (HorizontalPager auto-scroll 4s + page indicators)
   - BannerItem data class
   - TTBannerItem (gradient bg + mesh overlay)
   - TTGameCard (TapTap-style: cover + title + rating + FilledTonalButton)
   - TTSectionHeader (title + optional icon + optional trailing)
   - TTVerifiedBadge

### Modified
3. `app/build.gradle`
   - versionCode 67 → 68
   - versionName '1.1.8-fix-rating-glitch' → '1.2.0-taptap-ui'
   - compose-bom 2024.10.00 → 2024.12.01
   - Added: coil-compose 2.7.0, lottie-compose 6.5.2, compose-shimmer 1.3.2

4. `app/src/main/java/com/drmacze/f16launcher/ModernUI.kt`
   - Removed duplicate color tokens (Carbon, GlassBase, Surface2, CandyCyan,
     CandyBlue, NeonGreen, SoftText, SubText, GlassStroke, DangerRed, AmberWarn)
     — sekarang hanya ada di TapTapDesignSystem.kt
   - Kept Premium* colors (PremiumBg, PremiumSurface, PremiumSurfaceHi,
     PremiumGold, PremiumViolet) sebagai extend palette

5. `app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt`
   - **FloatingNav**: rombak ke pill shape (rounded 999), active filled bg +
     scale animation, animateDpAsState untuk indicator padding, flatter
     shadowElevation (24 → 16.dp)
   - **HomeScreen**:
     - Added TTBannerCarousel (3 banners: FIFA 16 Mobile cyan-blue, DLavie 26
       violet-cyan, Komunitas green-blue) — placed after existing hero banner
     - Replaced PremiumGlassCard game card with TTGameCard (TapTap-style)
       wrapped in Column untuk inline download progress + error display
     - Replaced "Trusted by DLavie" Row with TTSectionHeader
     - Replaced SetupState.LOADING spinner GlassCard with TTGameCardSkeleton (3x)
     - Kept: maintenance banner, notification banner, top bar, hero banner
       (typewriter + shiny title), rating popup button, setup cards
       (NEED_GAME/NEED_DATA/READY), status chips, feed, pull-to-refresh
   - **UpdateScreen**:
     - Wrapped patch card in `if (loading) { TTGameCardSkeleton() x2 } else { GlassCard }`
     - Saat fetchUpdateInfo in progress → shimmer skeletons
   - **ProfileScreen**:
     - Added profileLoading state (true initially, false after gameInstalled loads)
     - When loading: TTGameCardSkeleton placeholder
     - When loaded: existing PremiumGlassCard avatar card
     - Made avatar Box clickable → expandedSection = "profile" (lifted state)
     - Added "Tap avatar untuk edit profil ↓" hint text
     - Refactored AccountSettingsCard: lifted expandedSection state via params
       (expandedSection + onExpandedSectionChange)

## Build Status
- Initial commit pending → GitHub Actions workflow `build-debug-apk.yml` akan trigger
- Build akan diverify via workflow run

## Constraints Honored
- ✓ Tidak hapus: auth logic, Supabase config, maintenance logic, PIN lock, telemetry, rating system
- ✓ Tidak ubah: Supabase URL/key, manifest, GitHub Actions workflow
- ✓ Hapus duplicate design tokens (single source: TapTapDesignSystem.kt)
- ✓ All real data (no dummy) — banners pakai real accent colors, TTGameCard pakai real rating data

## Phase 2 Readiness
Setelah Phase 1 merge, siap untuk Phase 2:
- **Lottie animations**: dependency sudah di-add (lottie-compose 6.5.2), tinggal
  add raw/*.json files + LottieAnimatable di hero banner / onboarding
- **Shared elements**: Compose BOM 2024.12.01 sudah support SharedTransitionLayout
  (experimental API) — bisa dipakai untuk hero → detail game transition
- **Video autoplay**: Coil 2.7.0 support VideoPlayer via AsyncImage + custom
  factory, atau pakai Media3 ExoPlayer untuk banner video
- **Coil image loading**: AsyncImage sudah siap dipakai untuk fetch cover game
  dari URL (saat ini TTGameCard pakai gradient + text cover, bisa di-upgrade
  ke AsyncImage untuk real cover images)
