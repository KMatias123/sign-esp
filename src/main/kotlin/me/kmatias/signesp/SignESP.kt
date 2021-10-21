package me.kmatias.signesp

import com.lambda.client.event.events.RenderOverlayEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.setting.settings.impl.collection.CollectionSetting
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.graphics.GlStateUtils
import com.lambda.client.util.graphics.ProjectionUtils
import com.lambda.client.util.graphics.font.FontRenderAdapter
import com.lambda.client.util.math.VectorUtils.toVec3dCenter
import com.lambda.client.util.threads.safeListener
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import org.lwjgl.opengl.GL11
import java.util.*

internal object SignESP : PluginModule(
    name = "SignESP",
    category = Category.RENDER,
    description = "Renders text on signs so u can see them without needing to freecam",
    pluginMain = Main
) {
    private val textScale by setting("TextScale", 1f, 0.1f..10f, 0.1f)

    private val tracers by setting("Tracers", false)
    private val tracerWidth by setting("TracerWidth", 2f, 1f..10f, 0.1f, { tracers })
    private val tracerAlpha by setting("TracerAlpha", 255, 0..255, 1, { tracers })

    // todo: when changing lists in clickgui drops we may want to make this default to true
    private val wordTracers by setting("WordTracers", false, { tracers })
    private val wordList = setting(CollectionSetting("WordList", mutableListOf("SalC1"), { false }))
    private val ignoreCase = setting("IgnoreCase", true, { wordTracers })

    private val showPosition by setting("ShowPosition", false)
    private val showDistance by setting("ShowDistance", false)
    private val yOffSet by setting("YOffset", 0.0, -5.0..5.0, 0.1)

    private val renderer = ESPRenderer()

    init {
        safeListener<RenderOverlayEvent> {
            for (tile in mc.world.loadedTileEntityList) {
                if (tile is TileEntitySign) {
                    renderText(tile.pos, tile.signText)
                }
            }
        }

        safeListener<RenderWorldEvent> {
            if (!tracers) {
                return@safeListener
            }

            for (tile in mc.world.loadedTileEntityList) {
                if (tile is TileEntitySign) {
                    renderTracers(tile.pos, tile.signText)
                }
            }
        }
    }

    private fun renderText(pos: BlockPos, texts: Array<ITextComponent>) {

        // text nametag thingies
        GlStateUtils.rescaleActual()
        GL11.glPushMatrix()
        val vecCenterPosShifted = pos.toVec3dCenter().add(0.0, yOffSet, 0.0)
        val screenPos = ProjectionUtils.toScreenPos(vecCenterPosShifted)
        GL11.glTranslated(screenPos.x, screenPos.y, 0.0)
        GL11.glScalef(textScale * 2, textScale * 2, 0f)

        val color = ColorHolder(255, 255, 255, 255)

        val rowsToDraw = ArrayList<String>()

        for (text in texts) {
            rowsToDraw.add(text.unformattedText)
        }

        if (showDistance) {
            rowsToDraw.add("distance: ${pos.toVec3dCenter().distanceTo(mc.player.position.toVec3dCenter())}")
        }
        if (showPosition) {
            rowsToDraw.add("coordinates: ${pos.toVec3dCenter()}")
        }

        rowsToDraw.forEachIndexed { index, text ->
            val halfWidth = FontRenderAdapter.getStringWidth(text) / -2.0f
            FontRenderAdapter.drawString(text, halfWidth, (FontRenderAdapter.getFontHeight() + 2.0f) * index, color = color)
        }
        GlStateUtils.rescaleMc()
        GL11.glPopMatrix()
    }

    private fun renderTracers(pos: BlockPos, texts: Array<ITextComponent>) {

        val rowsToDraw = ArrayList<String>()

        for (text in texts) {
            rowsToDraw.add(text.unformattedText)
        }
        GL11.glPushMatrix()

        renderer.aTracer = tracerAlpha
        renderer.thickness = tracerWidth

        if (wordTracers) {
            var flagged = false
            for (text in rowsToDraw) {
                for (match in wordList) {

                    if ((ignoreCase.value && text.lowercase(Locale.getDefault()).contains(match.lowercase(Locale.getDefault()))) || text.contains(match)) {
                        flagged = true
                    }
                }
            }

            if (flagged) {
                renderer.add(pos, ColorHolder(255, 255, 255, tracerAlpha))
            }
        } else {
            renderer.add(pos, ColorHolder(255, 255, 255, tracerAlpha))
        }
        renderer.render(true)

        GL11.glPopMatrix()
    }
}