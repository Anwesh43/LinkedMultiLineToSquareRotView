package com.anwesh.uiprojects.multilinetosquarerotview

/**
 * Created by anweshmishra on 07/07/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#1A237E")
val backColor : Int = Color.parseColor("#BDBDBD")
val sweepDeg : Float = 90f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float {
    return mirrorValue(a, b) * dir * scGap
}

fun Canvas.drawSquare(i : Int, sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    val xGap : Float = size / lines
    save()
    rotate(sweepDeg * sc2.divideScale(i, lines))
    for (j in 0..1) {
        val y : Float = xGap * (1f - 2 * j) * sc1.divideScale(j, lines)
        drawLine(-xGap, y, xGap, y, paint)
        drawLine(-xGap, 0f, -xGap, y, paint)
        drawLine(xGap, 0f, xGap, y, paint)
    }
    restore()
}

fun Canvas.drawMultiLineSquare(sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        drawSquare(j, sc1, sc2, size, paint)
    }
}

fun Canvas.drawMLTSRNode(i : Int, scale : Float, paint : Paint) {
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    save()
    translate(w / 2, gap * (i + 1))
    drawMultiLineSquare(sc1, sc2, size, paint)
    restore()
}

class MultiLineToSquareView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
                return true
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class MLTSRNode(var i : Int, val state : State = State()) {

        private var next : MLTSRNode? = null
        private var prev : MLTSRNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = MLTSRNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawMLTSRNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : MLTSRNode {
            var curr : MLTSRNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class MultiLineToSquare(var i : Int) {

        private val root : MLTSRNode = MLTSRNode(0)
        private var curr : MLTSRNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : MultiLineToSquareView) {

        private val animator : Animator = Animator(view)
        private val mltsr : MultiLineToSquare = MultiLineToSquare(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            mltsr.draw(canvas, paint)
            animator.animate {
                mltsr.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            mltsr.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : MultiLineToSquareView {
            val view : MultiLineToSquareView = MultiLineToSquareView(activity)
            activity.setContentView(view)
            return view 
        }
    }
}
