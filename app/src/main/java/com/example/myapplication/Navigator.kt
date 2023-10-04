package com.example.myapplication

import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Segment
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.transport.masstransit.Route
import kotlin.math.min

class Navigator {
    data class Result(val type: ResultType, val dist: Int = 0, val heading: Int = 0, val debugStr : String = "") {
        enum class ResultType { Ignore, LowAccuracy, LowSpeed, Finished, Proceed }
    }

    enum class Status { NoRoute, Wait, OnRoute, LostRoute, Finished }
    var status = Status.NoRoute
    private val D_ACC =  7.0  // Accuracy
    private val D_TARG = D_ACC  // Arrival
    private val D_SNAP = D_ACC  // Close to route
    private val D_LOST = D_SNAP * 1.5  // Lost route
    private val D_SPEED = 0.4  // Speed
    private val D_TOOFAR = 200 // do not look for the closest that far
    private var ipoint = 0
    //private var iseg = 0

    var route: Route? = null
        set(value) {
            field = value
            status = if (value == null || value.geometry.points.size < 2) Status.NoRoute else Status.Wait
            ipoint = 0
            //iseg = 0
        }

    fun update(location: Location) : Result {
        if (status == Status.NoRoute || status == Status.Finished || route == null)
            return Result(Result.ResultType.Ignore)

        if (location.accuracy == null || location.accuracy!! > D_ACC) {
            return Result(Result.ResultType.LowAccuracy)
        }
        if (location.speed == null || location.heading == null || location.speed!! < D_SPEED) {
            return Result(Result.ResultType.LowSpeed)
        }

        val pos = location.position
        val points = route!!.geometry.points
        var cpi = 0
        var cdist = (Geo::distance)(pos, points[0])
        val tolerance = min(location.accuracy!!, D_ACC)

        //points.forEachIndexed { i, p ->
        for (i in ipoint until points.size) {
            val p = points[i]
            val d = (Geo::distance)(pos, p)
            if (d < cdist || d < tolerance) { // thus we will find the most ahead laying close point
                cdist = d
                cpi = i
            }
        }
        ipoint = cpi //!!!

        var cseg = when (cpi) {
            0 -> 0
            points.size - 1 -> points.size - 2
            else -> {
                val dprev = (Geo::closestPoint)(pos, Segment(points[cpi - 1], points[cpi]))
                val dnext = (Geo::closestPoint)(pos, Segment(points[cpi], points[cpi + 1]))
                if ((Geo::distance)(pos, dprev) < (Geo::distance)(pos, dnext)) cpi - 1
                else cpi
            }
        }
        val spoint = (Geo::closestPoint)(
            pos,
            Segment(points[cseg], points[cseg + 1])
        ) // closest point on seg
        val sdist = (Geo::distance)(pos, spoint)
        val tpi = cseg + 1 // target
        var tpoint = points[tpi] //next point on closest segment
        ipoint = cseg

        when (status) {
            Status.Wait, Status.LostRoute -> {
                if (sdist < D_SNAP) { //On route
                    status = Status.OnRoute
                } else {
                    status = Status.LostRoute
                    tpoint = spoint // target to the closest seg
                }
            }

            Status.OnRoute -> {
                if (sdist < D_LOST) { //On route
                    if (cpi == points.size - 1 && cdist <= D_TARG) {
                        status = Status.Finished
                        return Result(Result.ResultType.Finished)
                    }
                    // follow...
                } else { // lost route...
                    status = Status.LostRoute
                    tpoint = spoint // target to the closest seg
                }
            }
            else -> {
                // ignore (NoRoute, Finished)
            }
        }

        var tdist = (Geo::distance)(pos, tpoint).toInt()
        val tcourse = (Geo::course)(pos, tpoint) // course: angle from NORTH to Vec(pos->p0)
        var heading = (tcourse - location.heading!!).toInt()

        var debugText =
            "$cpi (${cdist.toInt()}), $cseg (${cdist.toInt()}), $tpi ($tdist $heading) $status}"

        return Result(Result.ResultType.Proceed, tdist, heading, debugText)
    }
/*
    fun update(location: Location) : Result {
        if (status == Status.NoRoute || status == Status.Finished || route == null)
            return Result(Result.ResultType.Ignore)

        if (location.accuracy == null || location.accuracy!! > D_ACC) {
            return Result(Result.ResultType.LowAccuracy)
        }
        if (location.speed == null || location.heading == null || location.speed!! < D_SPEED) {
            return Result(Result.ResultType.LowSpeed)
        }

        val pos = location.position
        val points = route!!.geometry.points
        var cseg = iseg
        var sdist = ((Geo::distance)(pos, (Geo::closestPoint)(pos, Segment(points[cseg], points[cseg+1]))))
        val tolerance = min(location.accuracy!!, D_ACC)

        for (i in iseg+1 until points.size-1) {
            val d = ((Geo::distance)(pos, (Geo::closestPoint)(pos, Segment(points[i], points[i+1]))))
            if (d > D_TOOFAR) break
            if (d < sdist || d < tolerance) { // thus we will find the most ahead laying close point //min???
                sdist = d
                cseg = i
            }
        }

        var tpoint = (Geo::closestPoint)(
            pos,
            Segment(points[cseg], points[cseg + 1])
        ) // closest point on seg
        var spoint = points[cseg + 1] // next point on seg

        when (status) {
            Status.Wait, Status.LostRoute -> {
                if (sdist < D_SNAP) { //On route
                    status = Status.OnRoute
                    tpoint = spoint // target to the next point on seg
                } else {
                    status = Status.LostRoute
                    // offroute - target to the closest seg (tpoint)
                }
            }

            Status.OnRoute -> {
                if (sdist < D_LOST) { //On route
                    if (cseg == points.size - 2 && (Geo::distance)(pos, spoint) <= D_TARG) {
                        status = Status.Finished
                        return Result(Result.ResultType.Finished)
                    }
                    // follow...
                } else { // lost route...
                    status = Status.LostRoute
                    tpoint = spoint // target to the next point on seg
                }
            }
            else -> {
                // ignore (NoRoute, Finished)
            }
        }


        var tdist = (Geo::distance)(pos, tpoint).toInt()
        val tcourse = (Geo::course)(pos, tpoint) // course: angle from NORTH to Vec(pos->p0)
        var heading = (tcourse - location.heading!!).toInt()

        var debugText =
            "$iseg -> $cseg, ${sdist.toInt()}, ($tdist $heading) $status}"

        this.iseg = cseg // save

        return Result(Result.ResultType.Proceed, tdist, heading, debugText)
    }

 */
}