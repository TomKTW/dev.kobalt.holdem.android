package dev.kobalt.holdem.android.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import dev.kobalt.holdem.android.R
import dev.kobalt.holdem.android.base.BaseView
import dev.kobalt.holdem.android.databinding.ViewPlayerBinding
import dev.kobalt.holdem.android.state.HoldemCard
import dev.kobalt.holdem.android.state.StateEntity

class PlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseView<ViewPlayerBinding>(context, attrs, defStyleAttr) {

    fun apply(entity: StateEntity.Player) {
        viewBinding.nameLabel.text = if (entity.money != null) {
            "${entity.name} (${entity.money})" + " (D)".takeIf { entity.tags.contains("dealer") }
                .orEmpty()
        } else {
            entity.name
        }
        viewBinding.cardStack.removeAllViews()
        entity.hand.forEach {
            viewBinding.cardStack.addView(SVGImageView(context).apply {
                it.src?.let { HoldemCard.valueOf(it) }?.let {
                    setSVG(SVG.getFromAsset(context.assets, "${it.image}.svg"))
                }
            }, dp(48), dp(48))
        }
        viewBinding.actionLabel.text = when {
            entity.action == "Idle" && (entity.betMoney == 0 || entity.betMoney == null) -> {
                ""
            }
            entity.betMoney == 0 || entity.betMoney == null -> {
                entity.action
            }
            else -> {
                "${entity.action} (${entity.betMoney})"
            }
        }
        viewBinding.actionLabel.isVisible = entity.action.orEmpty().isNotEmpty()
        if (entity.tags.contains("current")) {
            background = ColorDrawable(getResourceColor(R.color.black_a50))
        }
        if (entity.tags.contains("winner")) {
            background = ColorDrawable(getResourceColor(R.color.white_a50))
        }
    }

}