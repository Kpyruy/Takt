# Takt Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the approved visual preferences, Material 3 design tokens, reusable card/header/navigation primitives, and edge-to-edge app shell before any feature screen is redesigned.

**Architecture:** Persist visual preferences in the existing `AppSettingsRepository`; keep preference enums in `:core:model` and make `:core:ui` depend on `:core:model` so the theme can consume them directly. Provide theme/card/navigation behavior through reusable Compose components and CompositionLocals so feature modules do not each reimplement preference logic.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, SharedPreferences, Coroutines/Flow, Java 17.

---

### Task 1: Add visual preference model tests

**Files:**
- Modify: `core/model/src/test/kotlin/com/kpyruy/takt/core/model/AppSettingsTest.kt`
- Modify: `core/data/src/test/java/com/kpyruy/takt/core/data/StoredSettingsCodecTest.kt`

- [ ] **Step 1: Add failing defaults test**

Append to `AppSettingsTest.kt`:

```kotlin
@Test
fun visualPreferencesHaveApprovedDefaults() {
    val settings = AppSettings()

    assertEquals(CardAppearance.ELEVATED, settings.cardAppearance)
    assertEquals(ThemeFamily.BLUE, settings.themeFamily)
    assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
    assertEquals(WeekLayout.TIMETABLE, settings.weekLayout)
}
```

- [ ] **Step 2: Add failing codec test**

Add to `StoredSettingsCodecTest.kt`:

```kotlin
@Test
fun decodeRestoresVisualPreferences() {
    val settings = StoredSettingsCodec.decode(
        cancellationStyle = "MARKED",
        showHiddenLessons = true,
        parityOverride = "ODD",
        cardAppearance = "TONAL_FILLED",
        themeFamily = "WARM",
        themeMode = "DARK",
        weekLayout = "COMPACT_LIST",
    )

    assertEquals(CardAppearance.TONAL_FILLED, settings.cardAppearance)
    assertEquals(ThemeFamily.WARM, settings.themeFamily)
    assertEquals(AppThemeMode.DARK, settings.themeMode)
    assertEquals(WeekLayout.COMPACT_LIST, settings.weekLayout)
}
```

Add imports:

```kotlin
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
```

- [ ] **Step 3: Run tests to verify failure**

Run:

```bash
gradle :core:model:test --stacktrace
gradle :core:data:testDebugUnitTest --stacktrace
```

Expected: compilation fails because the new enums/fields and codec parameters do not exist yet.

---

### Task 2: Implement and persist visual preferences

**Files:**
- Modify: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/AppSettings.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/AppSettingsRepository.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/StoredSettingsCodec.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/SharedPreferencesAppSettingsRepository.kt`

- [ ] **Step 1: Add preference enums and fields**

Add above `AppSettings` in `AppSettings.kt`:

```kotlin
enum class CardAppearance {
    ELEVATED,
    TONAL_FILLED,
}

enum class ThemeFamily {
    BLUE,
    GREEN,
    PURPLE,
    WARM,
    MONOCHROME,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class WeekLayout {
    TIMETABLE,
    COMPACT_LIST,
}
```

Extend `AppSettings`:

```kotlin
data class AppSettings(
    val cancellationStyle: CancellationDisplayStyle = CancellationDisplayStyle.STRIKETHROUGH,
    val showHiddenLessons: Boolean = false,
    val parityOverride: ParityOverride = ParityOverride.AUTO,
    val cardAppearance: CardAppearance = CardAppearance.ELEVATED,
    val themeFamily: ThemeFamily = ThemeFamily.BLUE,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val weekLayout: WeekLayout = WeekLayout.TIMETABLE,
) {
```

- [ ] **Step 2: Extend repository interface**

Add to `AppSettingsRepository`:

```kotlin
suspend fun setCardAppearance(appearance: CardAppearance)
suspend fun setThemeFamily(themeFamily: ThemeFamily)
suspend fun setThemeMode(themeMode: AppThemeMode)
suspend fun setWeekLayout(layout: WeekLayout)
```

Add imports for the four enums.

- [ ] **Step 3: Extend codec**

Replace `decode` signature with:

```kotlin
fun decode(
    cancellationStyle: String?,
    showHiddenLessons: Boolean,
    parityOverride: String?,
    cardAppearance: String? = null,
    themeFamily: String? = null,
    themeMode: String? = null,
    weekLayout: String? = null,
): AppSettings {
    val default = AppSettings()
    return AppSettings(
        cancellationStyle = enumValueOrNull<CancellationDisplayStyle>(cancellationStyle)
            ?: default.cancellationStyle,
        showHiddenLessons = showHiddenLessons,
        parityOverride = enumValueOrNull<ParityOverride>(parityOverride)
            ?: default.parityOverride,
        cardAppearance = enumValueOrNull<CardAppearance>(cardAppearance)
            ?: default.cardAppearance,
        themeFamily = enumValueOrNull<ThemeFamily>(themeFamily)
            ?: default.themeFamily,
        themeMode = enumValueOrNull<AppThemeMode>(themeMode)
            ?: default.themeMode,
        weekLayout = enumValueOrNull<WeekLayout>(weekLayout)
            ?: default.weekLayout,
    )
}
```

- [ ] **Step 4: Persist new keys**

Add methods to `SharedPreferencesAppSettingsRepository`:

```kotlin
override suspend fun setCardAppearance(appearance: CardAppearance) {
    preferences.edit().putString(KEY_CARD_APPEARANCE, appearance.name).apply()
    state.value = state.value.copy(cardAppearance = appearance)
}

override suspend fun setThemeFamily(themeFamily: ThemeFamily) {
    preferences.edit().putString(KEY_THEME_FAMILY, themeFamily.name).apply()
    state.value = state.value.copy(themeFamily = themeFamily)
}

override suspend fun setThemeMode(themeMode: AppThemeMode) {
    preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
    state.value = state.value.copy(themeMode = themeMode)
}

override suspend fun setWeekLayout(layout: WeekLayout) {
    preferences.edit().putString(KEY_WEEK_LAYOUT, layout.name).apply()
    state.value = state.value.copy(weekLayout = layout)
}
```

Pass stored values into `StoredSettingsCodec.decode`:

```kotlin
cardAppearance = preferences.getString(KEY_CARD_APPEARANCE, null),
themeFamily = preferences.getString(KEY_THEME_FAMILY, null),
themeMode = preferences.getString(KEY_THEME_MODE, null),
weekLayout = preferences.getString(KEY_WEEK_LAYOUT, null),
```

Add keys:

```kotlin
const val KEY_CARD_APPEARANCE = "card_appearance"
const val KEY_THEME_FAMILY = "theme_family"
const val KEY_THEME_MODE = "theme_mode"
const val KEY_WEEK_LAYOUT = "week_layout"
```

- [ ] **Step 5: Run model/data tests**

Run:

```bash
gradle :core:model:test :core:data:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add core/model core/data
git commit -m "feat: add persisted visual preferences"
```

---

### Task 3: Build theme tokens and five color families

**Files:**
- Modify: `core/ui/build.gradle.kts`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/theme/TaktPalette.kt`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/theme/TaktTypography.kt`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/theme/TaktShapes.kt`
- Modify: `core/ui/src/main/java/com/kpyruy/takt/core/ui/theme/TaktTheme.kt`

- [ ] **Step 1: Make UI depend on model**

Add to `core/ui/build.gradle.kts` dependencies:

```kotlin
implementation(project(":core:model"))
```

- [ ] **Step 2: Create controlled palettes**

Create `TaktPalette.kt`:

```kotlin
package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.ThemeFamily

internal data class TaktPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val subjectColorsLight: List<Color>,
    val subjectColorsDark: List<Color>,
)

private fun palette(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    darkPrimary: Color,
    darkSecondary: Color,
    darkTertiary: Color,
    subjectsLight: List<Color>,
    subjectsDark: List<Color>,
): TaktPalette = TaktPalette(
    light = lightColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = Color(0xFFF7F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF0F2F7),
        onSurface = Color(0xFF111827),
        onSurfaceVariant = Color(0xFF667085),
    ),
    dark = darkColorScheme(
        primary = darkPrimary,
        secondary = darkSecondary,
        tertiary = darkTertiary,
        background = Color(0xFF0B1220),
        surface = Color(0xFF111A2B),
        surfaceVariant = Color(0xFF182338),
        onSurface = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFFADB8CA),
    ),
    subjectColorsLight = subjectsLight,
    subjectColorsDark = subjectsDark,
)

internal fun paletteFor(family: ThemeFamily): TaktPalette = when (family) {
    ThemeFamily.BLUE -> palette(
        primary = Color(0xFF3659C9),
        secondary = Color(0xFF27695D),
        tertiary = Color(0xFF7454B8),
        darkPrimary = Color(0xFF8DA2FF),
        darkSecondary = Color(0xFF94D3C5),
        darkTertiary = Color(0xFFC7A9FF),
        subjectsLight = listOf(
            Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFF8B5CF6),
            Color(0xFFF59E0B), Color(0xFFEF5DA8), Color(0xFF22A06B),
        ),
        subjectsDark = listOf(
            Color(0xFF7AB6FF), Color(0xFF61D6C6), Color(0xFFB69CFF),
            Color(0xFFFFC761), Color(0xFFFF92C5), Color(0xFF69D5A3),
        ),
    )
    ThemeFamily.GREEN -> palette(
        primary = Color(0xFF147D64),
        secondary = Color(0xFF356B9A),
        tertiary = Color(0xFF9A6717),
        darkPrimary = Color(0xFF70D6B6),
        darkSecondary = Color(0xFF8DC7F5),
        darkTertiary = Color(0xFFE7B85B),
        subjectsLight = listOf(
            Color(0xFF16A085), Color(0xFF3B82F6), Color(0xFF84A937),
            Color(0xFFF59E0B), Color(0xFF9B6BD3), Color(0xFFE05D6F),
        ),
        subjectsDark = listOf(
            Color(0xFF67D5BE), Color(0xFF7AB6FF), Color(0xFFB7D66E),
            Color(0xFFFFC761), Color(0xFFC5A0EC), Color(0xFFF194A1),
        ),
    )
    ThemeFamily.PURPLE -> palette(
        primary = Color(0xFF7048C8),
        secondary = Color(0xFF3A7198),
        tertiary = Color(0xFF9D5F7A),
        darkPrimary = Color(0xFFB49AF2),
        darkSecondary = Color(0xFF8CC5EC),
        darkTertiary = Color(0xFFE0A2BF),
        subjectsLight = listOf(
            Color(0xFF7C5CE0), Color(0xFF3B82F6), Color(0xFF00A68A),
            Color(0xFFE08035), Color(0xFFD9568A), Color(0xFF9A70B8),
        ),
        subjectsDark = listOf(
            Color(0xFFB69CFF), Color(0xFF7AB6FF), Color(0xFF65D4BD),
            Color(0xFFFFB66F), Color(0xFFF08CB2), Color(0xFFC8A0DF),
        ),
    )
    ThemeFamily.WARM -> palette(
        primary = Color(0xFFC65A2E),
        secondary = Color(0xFF9B6A19),
        tertiary = Color(0xFF8B4D72),
        darkPrimary = Color(0xFFF4A17E),
        darkSecondary = Color(0xFFE7C06B),
        darkTertiary = Color(0xFFDCA0C5),
        subjectsLight = listOf(
            Color(0xFFE36D3D), Color(0xFFD99A20), Color(0xFFB65D86),
            Color(0xFF6E8F3D), Color(0xFF4A7FB1), Color(0xFF9A6BC0),
        ),
        subjectsDark = listOf(
            Color(0xFFFFA17B), Color(0xFFFFC965), Color(0xFFE49BBD),
            Color(0xFFA9C97A), Color(0xFF86B7E5), Color(0xFFC7A1E2),
        ),
    )
    ThemeFamily.MONOCHROME -> palette(
        primary = Color(0xFF303846),
        secondary = Color(0xFF5C6573),
        tertiary = Color(0xFF7B8491),
        darkPrimary = Color(0xFFD5DBE5),
        darkSecondary = Color(0xFFADB7C5),
        darkTertiary = Color(0xFF8F9AAA),
        subjectsLight = listOf(
            Color(0xFF2F6B8A), Color(0xFF547A62), Color(0xFF7B6B9B),
            Color(0xFF9A734A), Color(0xFF855E72), Color(0xFF586B80),
        ),
        subjectsDark = listOf(
            Color(0xFF75A9C2), Color(0xFF83AB8E), Color(0xFFA79BC1),
            Color(0xFFC2A078), Color(0xFFB88FA4), Color(0xFF8EA1B4),
        ),
    )
}
```

- [ ] **Step 3: Create typography**

Create `TaktTypography.kt`:

```kotlin
package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val TaktTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
)
```

- [ ] **Step 4: Create shapes**

Create `TaktShapes.kt`:

```kotlin
package com.kpyruy.takt.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val TaktShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

- [ ] **Step 5: Replace theme**

Replace `TaktTheme.kt` with:

```kotlin
package com.kpyruy.takt.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance

val LocalTaktCardAppearance = staticCompositionLocalOf { CardAppearance.ELEVATED }
val LocalTaktSubjectColors = staticCompositionLocalOf<List<Color>> { emptyList() }

@Composable
fun TaktTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val palette = paletteFor(settings.themeFamily)

    CompositionLocalProvider(
        LocalTaktCardAppearance provides settings.cardAppearance,
        LocalTaktSubjectColors provides if (darkTheme) palette.subjectColorsDark else palette.subjectColorsLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) palette.dark else palette.light,
            typography = TaktTypography,
            shapes = TaktShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 6: Compile UI**

Run:

```bash
gradle :core:ui:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add core/ui
git commit -m "feat: establish Takt design tokens"
```

---

### Task 4: Refactor reusable card and header primitives

**Files:**
- Modify: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/SectionCard.kt`
- Modify: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/MetricCard.kt`
- Modify: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/ScreenHeader.kt`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/TaktSegmentedTabs.kt`

- [ ] **Step 1: Make SectionCard obey card preference**

Use `LocalTaktCardAppearance.current` and these values:

```kotlin
val appearance = LocalTaktCardAppearance.current
val colors = when (appearance) {
    CardAppearance.ELEVATED -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )
    CardAppearance.TONAL_FILLED -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}
val elevation = CardDefaults.cardElevation(
    defaultElevation = if (appearance == CardAppearance.ELEVATED) 2.dp else 0.dp,
)

Card(
    modifier = modifier.fillMaxWidth(),
    colors = colors,
    elevation = elevation,
    shape = MaterialTheme.shapes.medium,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
```

- [ ] **Step 2: Keep MetricCard compact**

Change the value style to `titleLarge`, retain label in `bodySmall`, and remove any hard-coded card colors so it inherits `SectionCard`.

- [ ] **Step 3: Make ScreenHeader Android-native**

Change `ScreenHeader` API to accept navigation separately:

```kotlin
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Render `navigation` on the left, title/subtitle in a weighted center column, and `action` on the right. Use `headlineMedium` for normal screen titles and allow wrapping to two lines.

- [ ] **Step 4: Add segmented tabs**

Create `TaktSegmentedTabs.kt`:

```kotlin
package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TaktSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                text = { Text(label, maxLines = 1) },
            )
        }
    }
}
```

- [ ] **Step 5: Compile**

Run:

```bash
gradle :core:ui:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add core/ui/src/main/java/com/kpyruy/takt/core/ui
git commit -m "feat: add reusable redesign components"
```

---

### Task 5: Enable edge-to-edge and drive theme from persisted settings

**Files:**
- Modify: `app/src/main/java/com/kpyruy/takt/app/MainActivity.kt`

- [ ] **Step 1: Wire settings into theme**

Replace `onCreate` content with:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val dataContainer = (application as TaktApplication).dataContainer
    setContent {
        val settings by dataContainer.settingsRepository.settings.collectAsState(
            initial = AppSettings()
        )

        TaktTheme(settings = settings) {
            TaktApp(
                repository = dataContainer.studyPlanRepository,
                scheduleRepository = dataContainer.scheduleRepository,
                gradeRepository = dataContainer.gradeRepository,
                studyContentRepository = dataContainer.studyContentRepository,
                settingsRepository = dataContainer.settingsRepository,
                backupRepository = dataContainer.backupRepository,
            )
        }
    }
}
```

Add imports:

```kotlin
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kpyruy.takt.core.model.AppSettings
```

- [ ] **Step 2: Assemble app**

Run:

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kpyruy/takt/app/MainActivity.kt
git commit -m "feat: enable edge to edge theming"
```

---

### Task 6: Add the approved visual settings controls

**Files:**
- Modify: `feature/settings/src/main/java/com/kpyruy/takt/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Fix back navigation placement**

Call `ScreenHeader` as:

```kotlin
ScreenHeader(
    title = "Налаштування",
    navigation = {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
        }
    },
)
```

- [ ] **Step 2: Add card appearance controls**

Add a section with:

```kotlin
SectionCard {
    Text("Картки", style = MaterialTheme.typography.titleMedium)
    listOf(
        CardAppearance.ELEVATED to "Підняті",
        CardAppearance.TONAL_FILLED to "Заливка",
    ).forEach { (option, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { settingsRepository.setCardAppearance(option) } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = settings.cardAppearance == option,
                onClick = { scope.launch { settingsRepository.setCardAppearance(option) } },
            )
            Text(label)
        }
    }
}
```

- [ ] **Step 3: Add five theme-family choices**

Add:

```kotlin
val themeLabels = listOf(
    ThemeFamily.BLUE to "Синя",
    ThemeFamily.GREEN to "Зелена",
    ThemeFamily.PURPLE to "Фіолетова",
    ThemeFamily.WARM to "Тепла",
    ThemeFamily.MONOCHROME to "Монохром",
)
```

Render them as radio rows and call `settingsRepository.setThemeFamily(option)`.

- [ ] **Step 4: Add light/dark/system controls**

Use:

```kotlin
listOf(
    AppThemeMode.SYSTEM to "Як у системі",
    AppThemeMode.LIGHT to "Світла",
    AppThemeMode.DARK to "Темна",
)
```

Call `settingsRepository.setThemeMode(option)`.

- [ ] **Step 5: Add week-view choice**

Use:

```kotlin
listOf(
    WeekLayout.TIMETABLE to "Таймтейбл",
    WeekLayout.COMPACT_LIST to "Компактний список",
)
```

Call `settingsRepository.setWeekLayout(option)`.

- [ ] **Step 6: Compile settings feature**

Run:

```bash
gradle :feature:settings:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add feature/settings
git commit -m "feat: expose redesign appearance settings"
```

---

### Task 7: Add a reusable FAB + four-tab bottom-navigation shell

**Files:**
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/TaktBottomNavigation.kt`
- Modify: `app/src/main/java/com/kpyruy/takt/app/TaktApp.kt`

- [ ] **Step 1: Create navigation component**

Create:

```kotlin
package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class TaktNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun TaktBottomNavigation(
    items: List<TaktNavItem>,
) {
    require(items.size == 4)

    NavigationBar {
        items.take(2).forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }

        Spacer(Modifier.width(72.dp))

        items.drop(2).forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun TaktAddFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Text("+", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
    }
}
```

- [ ] **Step 2: Replace direct NavigationBar construction**

In `TaktApp.kt`, build four `TaktNavItem` values from `Destination.entries`, render `TaktBottomNavigation` in `bottomBar`, and add a centered FAB only on root destinations:

```kotlin
val showRootNavigation = Destination.entries.any { it.route == currentRoute }
var quickAddRequested by rememberSaveable { mutableStateOf(false) }

Scaffold(
    bottomBar = {
        if (showRootNavigation) {
            TaktBottomNavigation(items = navItems)
        }
    },
    floatingActionButton = {
        if (showRootNavigation) {
            TaktAddFab(onClick = { quickAddRequested = true })
        }
    },
    floatingActionButtonPosition = FabPosition.Center,
) { padding ->
    // existing NavHost
}
```

For this foundation phase, consume the request with a small informational sheet so the FAB is functional until Phase 2 replaces it:

```kotlin
if (quickAddRequested) {
    ModalBottomSheet(onDismissRequest = { quickAddRequested = false }) {
        Text(
            "Швидке додавання",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Повний вибір типу елемента додається в наступному етапі.",
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp),
        )
    }
}
```

This text is a temporary implementation-state message only on the feature branch and must be replaced by Phase 2 before final delivery.

- [ ] **Step 3: Compile**

Run:

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add core/ui app
git commit -m "feat: add FAB navigation shell"
```

---

### Task 8: Foundation verification

**Files:** no source changes unless verification exposes defects.

- [ ] **Step 1: Run domain tests**

```bash
gradle :core:model:test --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Android unit tests**

```bash
gradle :core:data:testDebugUnitTest :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run lint**

```bash
gradle :app:lintDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL` with no fatal lint errors.

- [ ] **Step 4: Assemble APK**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Commit only verification-driven fixes**

If verification required source fixes:

```bash
git add -A
git commit -m "fix: stabilize redesign foundation"
```

Do not create an empty commit.
