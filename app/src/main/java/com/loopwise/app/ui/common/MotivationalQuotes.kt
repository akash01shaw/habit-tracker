package com.loopwise.app.ui.common

import java.time.LocalDate

/**
 * One quote per day, same quote across all 5 tabs (and stable if you background/
 * foreground the app) — deterministic from the date rather than random-per-render,
 * so it doesn't change every time a screen recomposes.
 */
object MotivationalQuotes {
    val all = listOf(
        "Discipline is choosing between what you want now and what you want most.",
        "Small daily improvements lead to staggering long-term results.",
        "You don't have to be extreme, just consistent.",
        "Motivation gets you going, but discipline keeps you growing.",
        "Every action you take is a vote for the person you wish to become.",
        "The secret of getting ahead is getting started.",
        "A habit is a formula our brain follows: cue, routine, reward.",
        "Success is the sum of small efforts repeated day in and day out.",
        "You are what you repeatedly do. Excellence is a habit, not an act.",
        "The chains of habit are too weak to be felt until they are too strong to be broken.",
        "First we make our habits, then our habits make us.",
        "Don't count the days, make the days count.",
        "It's not about having time, it's about making time.",
        "Progress, not perfection.",
        "The best time to plant a tree was 20 years ago. The second best time is now.",
        "What you do every day matters more than what you do once in a while.",
        "Habits are the compound interest of self-improvement.",
        "Discipline equals freedom.",
        "You will never always be motivated, so you must learn to be disciplined.",
        "The pain of discipline weighs ounces; the pain of regret weighs tons.",
        "Focus on being productive instead of busy.",
        "Little by little, a little becomes a lot.",
        "Your future is created by what you do today, not tomorrow.",
        "Consistency is what transforms average into excellence.",
        "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
        "The journey of a thousand miles begins with a single step.",
        "Starting today, you need to forgive yourself for not being who you wanted to be, and love yourself enough to start becoming them.",
        "Habits are first cobwebs, then cables.",
        "Do something today that your future self will thank you for.",
        "You don't have to see the whole staircase, just take the first step.",
    )

    fun forDate(date: LocalDate): String = all[Math.floorMod(date.toEpochDay(), all.size.toLong()).toInt()]
}
