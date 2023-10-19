package com.example.myapplication

import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Segment
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.transport.masstransit.Route
import kotlin.math.max
import kotlin.math.min

class Navigator {
    data class Result(val type: ResultType, val status: Status,
                      val dist: Int = 0, val heading: Int = 0,
                      val jump: Int? = null, val backJump : Boolean = false, val debugStr : String = "") {
        enum class ResultType { None, Ignore, LowAccuracy, LowSpeed, Finished, Proceed }
    }

    enum class Status { NoRoute, Wait, OnRoute, LostRoute, Finished }
    private var status = Status.NoRoute
    private val D_ACC =  6.0  // Accuracy - should be 4.
    private val D_TARG = D_ACC  // Arrival
    private val D_SNAP = D_ACC  // Close to route
    private val D_LOST = D_SNAP * 1.5  // Lost route
    private val D_JUMP = 2.5 // jump to next
    private val D_SPEED = 0.2  // Speed
    private val D_TOOFAR = 200 // do not look for the closest that far
    private var ipoint = 0
    private var prev_itarg = 0
    private var pos = Point(0.0, 0.0) // current location
    private var points : List<Point> = listOf()
    val DummyResult = Result(Result.ResultType.None, Status.NoRoute)


    var route: Route? = null
        set(value) {
            field = value
            status = if (value == null || value.geometry.points.size < 2) Status.NoRoute else Status.Wait
            ipoint = 0
            prev_itarg = 0
            points =  route!!.geometry.points
        }

    fun getDistanceTo(p: Point) = (Geo::distance)(pos, p)
    fun getClosestPointInSegment(iseg : Int) =
        (Geo::closestPoint)(pos, Segment(points[iseg], points[iseg+1]))
    fun getSegmentLength(iseg: Int) = (Geo::distance)(points[iseg], points[iseg + 1])

    fun getHeadingTo(heading: Double, target: Point) : Int {
        val tcourse = (Geo::course)(pos, target) // course: angle from NORTH to Vec(pos->p0)
        var heading = (tcourse - heading).toInt()
        if (heading < 0) {
            heading += 360
        }
        return  heading
    }

    fun update(location: Location) : Result {

        if (status == Status.NoRoute || status == Status.Finished || route == null)
            return Result(Result.ResultType.Ignore, status)

        if (location.accuracy == null || location.accuracy!! > D_ACC) {
            return Result(Result.ResultType.LowAccuracy, status)
        }
        if (location.speed == null || location.heading == null || location.speed!! < D_SPEED) {
            return Result(Result.ResultType.LowSpeed, status)
        }
        /*
        *  try: if accuracy degrades compar to prev (acc > acc_prev) but still within limits, use extrapolation
        * with prev course, speed and time passed
           */

        pos = location.position
        var cpi = 0
        var cdist = getDistanceTo(points[0])
        val tolerance = min(location.accuracy!!, D_ACC)

        for (i in ipoint until points.size) {
            val p = points[i]
            val d = getDistanceTo(p)
            if (d > D_TOOFAR && cdist < D_LOST) break
            if (d < cdist || d < tolerance) { // thus we will find the most ahead laying close point
                cdist = d
                cpi = i
            }
        }

        var cseg = when (cpi) {
            0 -> 0
            points.size - 1 -> points.size - 2
            else -> {
                val dprev = getClosestPointInSegment(cpi - 1)
                val dnext = getClosestPointInSegment(cpi)
                val prevseglen = getSegmentLength(cpi - 1)
                if (getDistanceTo(dprev) < getDistanceTo(dnext) && prevseglen > D_JUMP) cpi - 1
                else cpi
            }
        }
        val spoint = getClosestPointInSegment(cseg) // closest point on seg
        val sdist = getDistanceTo(spoint)
        var tpi = cseg + 1 // target
        var tpoint = points[tpi] //next point on closest segment
        var jump : Int?= null

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
                        return Result(Result.ResultType.Finished, status)
                    }
                    // follow...
                    if (tpi < points.size-1) {
                        val distToNext = getDistanceTo(tpoint)
                        val predict =
                            max(D_JUMP + location.speed!! * 2, location.accuracy!!) // min to max...
                        val heading = getHeadingTo(location.heading!!, tpoint)
                        if (distToNext < predict && (heading < 45 || heading > 315)) {
                            // jump to the next segment
                            tpi += 1
                            tpoint = points[tpi]
                            jump = distToNext.toInt()
                        }
                    }

                } else { // lost route...
                    status = Status.LostRoute
                    tpoint = spoint // target to the closest seg
                }
            }
            else -> {
                // ignore (NoRoute, Finished)
            }
        }

        var tdist = getDistanceTo(tpoint).toInt()
        val heading = getHeadingTo(location.heading!!, tpoint)

        //var debugText =
        //    "$cpi (${cdist.toInt()}), $cseg (${cdist.toInt()}), $tpi ($tdist $heading) $status}"

        val backJump = tpi < prev_itarg
        var debugText = "$cpi, $tpi, ${sdist.toInt()}, ($tdist $heading) $status $jump $backJump"

        ipoint = cpi
        prev_itarg = tpi

        return Result(Result.ResultType.Proceed, status, tdist, heading, jump, backJump, debugText)
    }
}