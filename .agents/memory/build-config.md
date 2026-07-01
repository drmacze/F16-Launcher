---
name: Build config and Supabase keys
description: How Supabase keys are stored in BuildConfig and which key goes where.
---

## BuildConfig fields (app/build.gradle defaultConfig)
- SUPABASE_URL      — shared URL for both auth systems
- SUPABASE_ANON_KEY — JWT anon key (eyJhbGci...), used in DLavieGuidedActivity
- SUPABASE_PUB_KEY  — publishable key (sb_publishable_...), used in CommunityApi.SUPABASE_KEY constant

## Why two different keys
Historical: different keys added at different times, both appear to work with the same Supabase project. Don't replace one with the other without testing both auth flows.

## Usage
- DLavieGuidedActivity.kt: `private val SUPABASE_URL get() = BuildConfig.SUPABASE_URL`
- CommunityApi.java: still uses hardcoded SUPABASE_KEY constant (not yet migrated to BuildConfig.SUPABASE_PUB_KEY — safe to migrate later)
- buildConfig = true must be in buildFeatures block
