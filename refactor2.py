with open('old_gaitscorer_utf8.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('package GaitVision.com.gait', 'package com.gaitvision.logic')
content = content.replace('import GaitVision.com.BuildConfig', '')
content = content.replace('import android.util.Log', 'import com.gaitvision.logic.Log')
content = content.replace('import android.os.Parcelable', 'import com.gaitvision.AppContext')
content = content.replace('import kotlinx.parcelize.Parcelize', '')
content = content.replace('class GaitScorer(private val context: Context)', 'actual class GaitScorer')
content = content.replace('context.assets', 'AppContext.context!!.assets')

lines = content.split('\n')
out_lines = []
skip = False
for line in lines:
    if 'data class ScoringResult' in line:
        skip = True
    if '@Parcelize' in line:
        pass
    elif skip:
        if line.startswith('}') and not 'fun' in line and not 'class' in line:
            # this is a weak check, but let\'s just strip scoringresult manually later
            pass
    
    if not skip and not '@Parcelize' in line:
        line = line.replace('if (BuildConfig.DEBUG) ', '')
        # remove override on score? No, it's not overriding anything in the expect class
        if 'actual class GaitScorer' in line:
            out_lines.append(line)
        elif 'fun initialize()' in line:
            out_lines.append(line.replace('fun initialize()', 'actual fun initialize()'))
        elif 'fun score(' in line:
            out_lines.append(line.replace('fun score(', 'actual fun score('))
        elif 'fun release()' in line:
            out_lines.append(line.replace('fun release()', 'actual fun release()'))
        else:
            out_lines.append(line)

final_out = '\n'.join(out_lines)

# Strip out ScoringResult from the bottom.
index = final_out.find('/**\n * Scoring results from all 3 models.')
if index != -1:
    final_out = final_out[:index]

with open('GaitVisionKMP/composeApp/src/androidMain/kotlin/com/gaitvision/logic/GaitScorer.android.kt', 'w', encoding='utf-8') as f:
    f.write(final_out)

print('Refactored again')
