package com.example.data

import com.example.R
import com.example.model.Channel
import com.example.model.NotificationItem
import com.example.model.ShortItem
import com.example.model.StudioSceneStyle
import com.example.model.ThumbnailArtwork
import com.example.model.VideoCategory
import com.example.model.VideoComment
import com.example.model.VideoItem

object SampleCatalog {

    val channels = listOf(
        Channel(
            id = "ch_apex_drive",
            name = "Apex Motion Lab",
            handle = "@apexmotion",
            baseSubscriberCount = 1_420_000,
            isVerified = true,
            avatarColorHex = 0xFFD91424,
            initials = "AM",
            category = VideoCategory.TECH,
            tagline = "Next-gen EV engineering, aerodynamics & industrial design."
        ),
        Channel(
            id = "ch_nordic_lens",
            name = "Nordic Lens 4K",
            handle = "@nordiclens",
            baseSubscriberCount = 890_500,
            isVerified = true,
            avatarColorHex = 0xFF047857,
            initials = "NL",
            category = VideoCategory.NATURE,
            tagline = "Ultra-high-definition expeditions across volcanic landscapes."
        ),
        Channel(
            id = "ch_pulse_synth",
            name = "Analog Pulse",
            handle = "@analogpulse",
            baseSubscriberCount = 615_200,
            isVerified = true,
            avatarColorHex = 0xFFB45309,
            initials = "AP",
            category = VideoCategory.MUSIC,
            tagline = "Live hardware synth sessions, sound design & studio tours."
        ),
        Channel(
            id = "ch_neon_frame",
            name = "Neon Frame Interactive",
            handle = "@neonframe",
            baseSubscriberCount = 2_180_000,
            isVerified = true,
            avatarColorHex = 0xFF7C3AED,
            initials = "NF",
            category = VideoCategory.GAMING,
            tagline = "Ray-traced worlds, game engine deep dives & sci-fi showcases."
        ),
        Channel(
            id = "ch_forma_studio",
            name = "Forma Spatial Design",
            handle = "@formadesign",
            baseSubscriberCount = 412_000,
            isVerified = false,
            avatarColorHex = 0xFF1D4ED8,
            initials = "FS",
            category = VideoCategory.DESIGN,
            tagline = "Craftsmanship in typography, motion systems & product UI."
        ),
        Channel(
            id = "ch_cosmos_brief",
            name = "Orbital Brief",
            handle = "@orbitalbrief",
            baseSubscriberCount = 1_050_000,
            isVerified = true,
            avatarColorHex = 0xFF0F766E,
            initials = "OB",
            category = VideoCategory.SCIENCE,
            tagline = "Astrophysics, propulsion systems & deep space telemetry."
        ),
        Channel(
            id = "ch_crimson_table",
            name = "The Crimson Table",
            handle = "@crimsontable",
            baseSubscriberCount = 328_400,
            isVerified = false,
            avatarColorHex = 0xFFBE123C,
            initials = "CT",
            category = VideoCategory.PODCASTS,
            tagline = "Uncut conversations with builders, directors & musicians."
        )
    )

    val videos = listOf(
        VideoItem(
            id = "vid_1",
            title = "Inside the Matte Titanium Hyper-EV: 0 to 200 in Silence",
            description = "We spent 72 hours inside the wind tunnel and test track with the prototype dual-axial electric hypercar. Full acoustic telemetry, battery thermal breakdown, and night studio showcase.",
            channelId = "ch_apex_drive",
            category = VideoCategory.TECH,
            viewsCount = 1_840_200,
            uploadDate = "4 hours ago",
            durationSeconds = 864,
            isTrending = true,
            trendingRank = 1,
            is4K = true,
            baseLikes = 94_200,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_tech_1794891410544),
            tags = listOf("EV", "Engineering", "4K", "Prototype"),
            comments = listOf(
                VideoComment("c1", "Maya Lin", "ML", 0xFFD91424, "That crimson rim lighting in the concrete hangar looks unreal.", "1h ago", 412),
                VideoComment("c2", "Devon Brooks", "DB", 0xFF1D4ED8, "The torque vectoring breakdown at 6:40 is the clearest explanation online.", "2h ago", 189)
            )
        ),
        VideoItem(
            id = "vid_2",
            title = "Iceland's Hidden Emerald Canyon — Golden Hour Drone Expedition",
            description = "Flying custom cinema drones through misty basalt canyons and glacial waterfalls in southern Iceland. Shot natively in 4K 60fps HDR with spatial field audio.",
            channelId = "ch_nordic_lens",
            category = VideoCategory.NATURE,
            viewsCount = 2_410_000,
            uploadDate = "1 day ago",
            durationSeconds = 1142,
            isTrending = true,
            trendingRank = 2,
            is4K = true,
            baseLikes = 148_500,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_nature_1794891410873),
            tags = listOf("Iceland", "Drone", "Documentary", "HDR"),
            comments = listOf(
                VideoComment("c3", "Soren Vester", "SV", 0xFF047857, "The mist rolling over the ridge at 4:15 gave me chills.", "5h ago", 620),
                VideoComment("c4", "Elena Rostova", "ER", 0xFFB45309, "Best natural soundscape on BT Tube this week.", "9h ago", 204)
            )
        ),
        VideoItem(
            id = "vid_3",
            title = "Midnight Analog Session: Building a Film Score on 5 Vintage Synths",
            description = "No laptops, no plugins—just warm analog oscillators, tape delay loops, and live modular patching in our midnight studio session.",
            channelId = "ch_pulse_synth",
            category = VideoCategory.MUSIC,
            viewsCount = 542_900,
            uploadDate = "2 days ago",
            durationSeconds = 975,
            isTrending = true,
            trendingRank = 3,
            baseLikes = 41_800,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_music_1794891411690),
            tags = listOf("Synthesizer", "LiveSession", "FilmScore"),
            comments = listOf(
                VideoComment("c5", "Jules Mercer", "JM", 0xFFB45309, "When that bassline drops around 3:20 it transforms the whole room.", "1d ago", 315)
            )
        ),
        VideoItem(
            id = "vid_4",
            title = "Rain-Slicked Neon Skylines: How Next-Gen Path Tracing Works",
            description = "Breaking down real-time reflections, volumetric rain shaders, and city-scale lighting budgets in modern interactive cyberpunk worlds.",
            channelId = "ch_neon_frame",
            category = VideoCategory.GAMING,
            viewsCount = 1_195_000,
            uploadDate = "3 days ago",
            durationSeconds = 738,
            isTrending = true,
            trendingRank = 4,
            is4K = true,
            baseLikes = 78_900,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_cyber_1794891411521),
            tags = listOf("RayTracing", "Graphics", "Cyberpunk", "DevLog"),
            comments = listOf(
                VideoComment("c6", "Kai Takahashi", "KT", 0xFF7C3AED, "Puddle reflections vs screen-space artifacts explained so cleanly!", "2d ago", 502)
            )
        ),
        VideoItem(
            id = "vid_5",
            title = "Designing Motion Systems That Feel Like Physical Glass",
            description = "Why spring physics, optical alignment, and micro-contrast make modern mobile interfaces feel alive instead of mechanical.",
            channelId = "ch_forma_studio",
            category = VideoCategory.DESIGN,
            viewsCount = 284_300,
            uploadDate = "5 days ago",
            durationSeconds = 612,
            baseLikes = 24_600,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.CRIMSON_STUDIO, "MOTION CRAFT"),
            tags = listOf("UIUX", "MotionDesign", "Typography"),
            comments = listOf(
                VideoComment("c7", "Aria Patel", "AP", 0xFF1D4ED8, "Applied these spring curves to our player controls right away.", "3d ago", 142)
            )
        ),
        VideoItem(
            id = "vid_6",
            title = "LIVE: Deep-Space Telemetry & Lunar Relay Launch Coverage",
            description = "Real-time mission commentary, orbital trajectory graphics, and live Q&A from our telemetry desk.",
            channelId = "ch_cosmos_brief",
            category = VideoCategory.SCIENCE,
            viewsCount = 918_400,
            uploadDate = "Streaming now",
            durationSeconds = 1800,
            isLive = true,
            baseLikes = 63_100,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.OBSIDIAN_TECH, "ORBIT LIVE"),
            tags = listOf("Space", "Live", "Telemetry"),
            comments = listOf(
                VideoComment("c8", "Dr. Nadia Okafor", "NO", 0xFF0F766E, "Telemetry lock looks super nominal through the perigee burn.", "12m ago", 88)
            )
        ),
        VideoItem(
            id = "vid_7",
            title = "Creative Directors Unfiltered: Why Restraint Wins Every Launch",
            description = "Episode 42 of The Crimson Table Podcast. Two veteran film and product directors discuss pacing, color discipline, and sound.",
            channelId = "ch_crimson_table",
            category = VideoCategory.PODCASTS,
            viewsCount = 196_700,
            uploadDate = "6 days ago",
            durationSeconds = 2640,
            baseLikes = 16_400,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.AMBER_ACOUSTIC, "EPISODE 42"),
            tags = listOf("Podcast", "CreativeDirection", "Interview"),
            comments = listOf(
                VideoComment("c9", "Liam Vance", "LV", 0xFFBE123C, "Best episode yet. The 18-minute mark on sound design is gold.", "4d ago", 94)
            )
        ),
        VideoItem(
            id = "vid_8",
            title = "Acoustic Science of Concert Halls vs Open Volcanic Valleys",
            description = "Comparing impulse responses recorded inside Nordic canyons with world-class timber symphony halls.",
            channelId = "ch_nordic_lens",
            category = VideoCategory.SCIENCE,
            viewsCount = 467_800,
            uploadDate = "1 week ago",
            durationSeconds = 790,
            is4K = true,
            baseLikes = 33_700,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.EMERALD_EXPEDITION, "ACOUSTICS"),
            tags = listOf("Acoustics", "Science", "FieldRecording"),
            comments = listOf()
        ),
        VideoItem(
            id = "vid_9",
            title = "Solid-State vs Silicon-Carbon Batteries: 12-Month Stress Test",
            description = "We cycled 12 flagship cells through extreme cold and rapid charging to measure real capacity retention.",
            channelId = "ch_apex_drive",
            category = VideoCategory.TECH,
            viewsCount = 735_000,
            uploadDate = "1 week ago",
            durationSeconds = 918,
            baseLikes = 51_200,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.OBSIDIAN_TECH, "LAB TEST"),
            tags = listOf("Battery", "TechReview", "Hardware"),
            comments = listOf()
        ),
        VideoItem(
            id = "vid_10",
            title = "Synthwave Rooftop Performance — Tokyo Rain Mix (Full Set)",
            description = "45 minutes of continuous live-looped analog leads and crisp drum machines recorded overlooking the neon skyline.",
            channelId = "ch_pulse_synth",
            category = VideoCategory.MUSIC,
            viewsCount = 1_045_000,
            uploadDate = "2 weeks ago",
            durationSeconds = 1420,
            baseLikes = 89_300,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.NEON_CYBER, "LIVE SET"),
            tags = listOf("Synthwave", "LiveMix", "Music"),
            comments = listOf()
        )
    )

    val shorts = listOf(
        ShortItem(
            id = "short_1",
            title = "Carbon Wheel Spin in Slow-Mo",
            caption = "1,200 FPS under crimson studio strobes. Zero CGI. #HyperEV #Engineering",
            channelId = "ch_apex_drive",
            baseLikes = 84_200,
            baseCommentsCount = 1_240,
            baseSharesCount = 4_810,
            audioTrack = "Apex Telemetry — Studio Pulse",
            durationSeconds = 28,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_tech_1794891410544),
            comments = listOf(
                VideoComment("sc1", "Theo Grant", "TG", 0xFFD91424, "The rim glow reflection is mesmerizing.", "20m ago", 142),
                VideoComment("sc2", "Nina Cho", "NC", 0xFF7C3AED, "Looping this 10 times!", "1h ago", 67)
            )
        ),
        ShortItem(
            id = "short_2",
            title = "Waterfall Drop at Dawn",
            caption = "60 meters of glacial mist catching the first sunlight in Iceland. #4K #Nature",
            channelId = "ch_nordic_lens",
            baseLikes = 132_900,
            baseCommentsCount = 2_410,
            baseSharesCount = 9_300,
            audioTrack = "Nordic Wind — Field Ambient",
            durationSeconds = 24,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_nature_1794891410873),
            comments = listOf(
                VideoComment("sc3", "Lukas Meyer", "LM", 0xFF047857, "Where exactly in the highlands was this filmed?", "3h ago", 88)
            )
        ),
        ShortItem(
            id = "short_3",
            title = "30-Second Analog Chord Trick",
            caption = "Detuning oscillator B by +7 cents instantly widens any pad. #Synth #MusicProd",
            channelId = "ch_pulse_synth",
            baseLikes = 49_600,
            baseCommentsCount = 690,
            baseSharesCount = 3_120,
            audioTrack = "Analog Pulse — Patch #09",
            durationSeconds = 30,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_music_1794891411690),
            comments = listOf(
                VideoComment("sc4", "Samira K.", "SK", 0xFFB45309, "Tried this on my polysynth and it sounded huge!", "5h ago", 54)
            )
        ),
        ShortItem(
            id = "short_4",
            title = "Neon Rain Shader Breakdown",
            caption = "How one normal map turns flat concrete into a wet city rooftop. #GameDev #3D",
            channelId = "ch_neon_frame",
            baseLikes = 96_500,
            baseCommentsCount = 1_530,
            baseSharesCount = 6_400,
            audioTrack = "Cyber Skyline — Original Mix",
            durationSeconds = 26,
            artwork = ThumbnailArtwork.PhotoDrawable(R.drawable.img_thumb_cyber_1794891411521),
            comments = listOf(
                VideoComment("sc5", "Rex Carter", "RC", 0xFF7C3AED, "Drop the shader graph tutorial next!", "1d ago", 210)
            )
        ),
        ShortItem(
            id = "short_5",
            title = "Kinetic Typography Micro-Tip",
            caption = "Tighten tracking by -2% on bold display headlines for instant editorial poise. #Design",
            channelId = "ch_forma_studio",
            baseLikes = 31_400,
            baseCommentsCount = 415,
            baseSharesCount = 1_950,
            audioTrack = "Forma Studio — Clicks & Grids",
            durationSeconds = 22,
            artwork = ThumbnailArtwork.VectorScene(StudioSceneStyle.CRIMSON_STUDIO, "TYPE TIP"),
            comments = listOf()
        )
    )

    val initialNotifications = listOf(
        NotificationItem(
            id = "n1",
            title = "Apex Motion Lab uploaded a 4K premier",
            subtitle = "Inside the Matte Titanium Hyper-EV: 0 to 200 in Silence",
            timeAgo = "4h ago",
            videoId = "vid_1",
            channelId = "ch_apex_drive",
            isRead = false
        ),
        NotificationItem(
            id = "n2",
            title = "Orbital Brief is LIVE now",
            subtitle = "Deep-Space Telemetry & Lunar Relay Launch Coverage",
            timeAgo = "Live",
            videoId = "vid_6",
            channelId = "ch_cosmos_brief",
            isRead = false
        ),
        NotificationItem(
            id = "n3",
            title = "Nordic Lens 4K posted a new expedition",
            subtitle = "Iceland's Hidden Emerald Canyon — Golden Hour Drone Expedition",
            timeAgo = "1d ago",
            videoId = "vid_2",
            channelId = "ch_nordic_lens",
            isRead = true
        )
    )

    val initialRecentSearches = listOf(
        "Hyper EV 4K",
        "Iceland drone canyon",
        "Analog synth session",
        "Ray tracing neon"
    )
}
