import re

with open('old_gaitscorer_utf8.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace package
content = content.replace('package GaitVision.com.gait', 'package com.gaitvision.logic')

# Remove Android/App specific imports that break KMP
content = content.replace('import GaitVision.com.BuildConfig', '')
content = content.replace('import android.util.Log', 'import com.gaitvision.logic.Log')
content = content.replace('import android.os.Parcelable', '')
content = content.replace('import kotlinx.parcelize.Parcelize', '')

# Remove BuildConfig.DEBUG
content = content.replace('if (BuildConfig.DEBUG) ', '')

# Replace constructor and add import
content = content.replace('class GaitScorer(private val context: Context)', 'import com.gaitvision.AppContext\n\nactual class GaitScorer')
content = content.replace('context.assets', 'AppContext.context!!.assets')

# Remove the data class ScoringResult since it's now in GaitModels.kt in commonMain
content = re.sub(r'/\*\*.*?class ScoringResult.*?\}.*?\}', '', content, flags=re.DOTALL)

with open('GaitVisionKMP/composeApp/src/androidMain/kotlin/com/gaitvision/logic/GaitScorer.android.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print('Refactoring complete')
