package uk.co.gazai

/**
 * Small, original starter knowledge pack. These notes are written for Gaz AI
 * and are not copied from copyrighted books. Each entry points to a source
 * where users can read more. New packs can be added without changing the model.
 */
data class KnowledgeEntry(
    val id: String,
    val collection: String,
    val title: String,
    val body: String,
    val sourceName: String,
    val sourceUrl: String,
    val sourceType: String = "Open reference"
)

object StarterLibrary {
    val entries = listOf(
        KnowledgeEntry("bible-overview", "Scripture", "The Bible: structure and context",
            "The Bible is a collection of writings produced across many centuries. The Christian Old Testament overlaps with the Jewish Tanakh but the ordering and canon differ among traditions. The New Testament contains four Gospels, Acts, letters and Revelation. A passage should be read in its literary, historical and cultural context; translations make interpretive choices, so comparing translations can reveal where wording is uncertain.",
            "World English Bible", "https://worldenglish.bible/"),
        KnowledgeEntry("bible-genres", "Scripture", "Reading biblical genres",
            "Biblical texts include narrative, law, poetry, prophecy, wisdom literature, Gospel, letter and apocalyptic writing. Genre affects interpretation: poetry uses imagery and parallelism; narrative describes events; letters address particular communities; apocalyptic writing uses symbolic visions. A sound reading distinguishes what a text says, what its first audience may have understood, and later theological interpretation.",
            "World English Bible", "https://worldenglish.bible/"),
        KnowledgeEntry("greek-nt", "Scripture", "Greek New Testament",
            "The New Testament was written in Koine Greek. Manuscripts differ in places, so modern editions compare manuscript evidence and record textual variants. A Greek word does not always have one fixed English equivalent: meaning depends on grammar, context, usage and the author's argument. Avoid building a doctrine on a word's possible etymology alone.",
            "SBL Greek New Testament", "https://www.sblgnt.com/", "CC BY 4.0"),
        KnowledgeEntry("hebrew-bible", "Scripture", "Hebrew Bible",
            "Most of the Hebrew Bible is written in Biblical Hebrew, with portions in Aramaic. Hebrew poetry often uses parallel lines, repeated ideas and metaphor. The Masoretic Text is a major medievally preserved textual tradition, while the Dead Sea Scrolls and ancient translations provide important comparative evidence.",
            "Open Scriptures Hebrew Bible", "https://github.com/openscriptures/morphhb", "Open-source project; check file licence"),
        KnowledgeEntry("strongs", "Scripture", "Strong's Concordance: how to use it",
            "Strong's numbers are identifiers that help readers find Hebrew and Greek words behind English Bible translations. They are useful for tracing occurrences, but they are not complete dictionary definitions. A number does not prove that a word has the same meaning in every passage. Check the original-language form, grammar, context and reputable lexicons.",
            "Strong's Concordance", "https://www.blueletterbible.org/lexicon/"),
        KnowledgeEntry("quran-context", "Scripture", "The Qur'an: context and structure",
            "The Qur'an is the central scripture of Islam and is arranged into 114 surahs. Surahs vary in length and are not arranged chronologically. Muslims regard the Arabic Qur'an as revelation; translations are generally understood as interpretations of its meaning. Study a passage with its surrounding verses, historical setting and recognised commentary, while noting differences among interpretive traditions.",
            "Tanzil Quran", "https://tanzil.net/", "Text use subject to Tanzil terms"),
        KnowledgeEntry("sikh-scripture", "Scripture", "Guru Granth Sahib",
            "The Guru Granth Sahib is the central scripture of Sikhism and is treated by Sikhs as the living Guru. It contains hymns by Sikh Gurus and other contributors, arranged by musical measure (raag). Translations can differ and cannot fully reproduce the nuances of the original languages. Identify the translator and edition when quoting or comparing a translation.",
            "Sri Guru Granth Sahib Project", "https://www.srigranth.org/", "Check individual translation terms"),
        KnowledgeEntry("dhammapada", "Scripture", "Dhammapada: themes",
            "The Dhammapada is a collection of short verses associated with Buddhist teaching. Recurring themes include ethical conduct, mental discipline, wisdom, impermanence and the consequences of intention and action. It is one text within a much larger Buddhist scriptural landscape; it should not be treated as representing every Buddhist school or teaching.",
            "SuttaCentral", "https://suttacentral.net/dhp", "Check translation licence on chosen edition"),
        KnowledgeEntry("plato", "Philosophy", "Plato and the examined life",
            "Plato presents philosophy largely through dialogues. Socrates questions assumptions and tests definitions rather than simply delivering a finished doctrine. The dialogues explore justice, knowledge, virtue, love and the nature of reality. Distinguish a character's argument from Plato's own settled view, and remember that scholars disagree about how the dialogues fit together.",
            "Perseus Digital Library", "https://www.perseus.tufts.edu/", "Public-domain editions"),
        KnowledgeEntry("aristotle", "Philosophy", "Aristotle: causes and inquiry",
            "Aristotle developed influential work in logic, biology, physics, ethics and politics. His four causes describe different kinds of explanation: material (what something is made of), formal (its organisation or form), efficient (what brings it about) and final (its end or function). These are historical philosophical concepts, not a substitute for modern scientific explanations.",
            "Internet Classics Archive", "http://classics.mit.edu/"),
        KnowledgeEntry("stoicism", "Philosophy", "Stoicism",
            "Stoicism is an ancient Greek and Roman philosophical tradition. It distinguishes what is within our control, such as judgements and intentions, from what is not fully within our control, such as reputation or external events. Stoic practice aims to develop wisdom, justice, courage and moderation. It does not require emotional numbness; it examines how judgements shape emotional responses.",
            "Internet Encyclopedia of Philosophy", "https://iep.utm.edu/stoicism/"),
        KnowledgeEntry("consciousness", "Science", "Consciousness: what science studies",
            "Consciousness research investigates subjective experience, wakefulness, attention, perception and self-awareness. Neuroscience can measure brain activity associated with conscious states, but there is no universally accepted explanation of why physical processes are accompanied by subjective experience. Distinguish measurable findings from philosophical interpretations and speculative theories.",
            "Stanford Encyclopedia of Philosophy", "https://plato.stanford.edu/entries/consciousness/"),
        KnowledgeEntry("dna", "Science", "DNA and genes",
            "DNA is a molecule that stores hereditary information using sequences of four nucleotide bases. A gene is a DNA sequence that contributes to a functional product, often a protein or functional RNA. Not all DNA codes for proteins, and traits usually arise through interactions among many genes, regulation, development and environment. Avoid describing genes as simple, fixed instructions for complex behaviour.",
            "National Human Genome Research Institute", "https://www.genome.gov/genetics-glossary"),
        KnowledgeEntry("evolution", "Science", "Evolution by natural selection",
            "Evolution is change in heritable characteristics of populations across generations. Natural selection occurs when heritable differences affect survival or reproduction in a particular environment. Evolution also involves mutation, genetic drift, gene flow and recombination. Individuals do not evolve during their lifetimes in the population-genetic sense; populations change over generations.",
            "Understanding Evolution, UC Berkeley", "https://evolution.berkeley.edu/"),
        KnowledgeEntry("brain", "Science", "The brain and nervous system",
            "The nervous system receives information, processes it and coordinates responses. Neurons communicate through electrical and chemical signalling, while glial cells support and regulate neural function. Brain functions arise from interacting networks rather than isolated single-purpose locations. Brain imaging shows associations and activity patterns; it does not by itself prove a simple cause.",
            "NINDS Brain Basics", "https://www.ninds.nih.gov/health-information/public-education/brain-basics"),
        KnowledgeEntry("physics-energy", "Science", "Energy in physics",
            "In physics, energy is a measurable property of a system that can be transferred or transformed. Conservation of energy means the total energy of an isolated system remains constant, although energy can change form and become less available to do useful work. The scientific meaning of energy is precise and should not automatically be equated with spiritual uses of the word.",
            "OpenStax Physics", "https://openstax.org/subjects/science", "Use linked materials subject to their individual licences"),
        KnowledgeEntry("symbolism", "Symbols", "How to investigate a symbol",
            "A symbol can carry different meanings in different periods and cultures. To investigate one, establish where and when it appears, who used it, what nearby images or words accompany it, and whether contemporary sources explain it. Similar shapes do not prove a shared origin or hidden connection. Separate documented historical meanings from later interpretations and personal associations.",
            "The British Museum collection", "https://www.britishmuseum.org/collection"),
        KnowledgeEntry("myth", "Symbols", "Comparative mythology",
            "Comparing myths can reveal recurring themes such as creation, flood, descent, transformation, sacrifice and renewal. Similar motifs may arise through cultural contact, shared human concerns or independent development. A parallel alone does not establish borrowing. Strong comparisons identify dates, sources, transmission routes and meaningful differences as well as similarities.",
            "Theoi Greek Mythology", "https://www.theoi.com/", "Reference catalogue; check individual source rights"),
        KnowledgeEntry("archaeology", "History", "Archaeology and evidence",
            "Archaeology studies past human societies through material remains and their contexts. Dating methods include stratigraphy, typology and scientific techniques such as radiocarbon dating. An artefact removed from its archaeological context loses important information. Interpretations should reflect the strength of the evidence and may change when new discoveries or methods become available.",
            "Archaeology Data Service", "https://archaeologydataservice.ac.uk/"),
        KnowledgeEntry("history-patterns", "History", "Patterns and cycles in history",
            "Historians identify recurring patterns such as migration, state formation, trade, conflict, environmental stress and technological change. Similarities between periods can be useful comparisons, but history does not necessarily repeat in a fixed cycle. Test a proposed pattern against chronology, causes, counterexamples and differences between societies.",
            "World History Encyclopedia", "https://www.worldhistory.org/"),
        KnowledgeEntry("research-method", "Research Skills", "How to weigh a claim",
            "For any claim, ask: what exactly is being asserted; what is the original source; is the evidence direct or indirect; can it be independently checked; what alternative explanations exist; and what evidence would change the conclusion? Give more weight to transparent methods, primary evidence and independent replication. Label fact, interpretation, hypothesis and speculation separately.",
            "UK Research and Innovation", "https://www.ukri.org/"),
        KnowledgeEntry("neville", "Philosophy", "Neville Goddard: interpretation and evidence",
            "Neville Goddard taught that imagination and assumption shape experience and interpreted biblical stories psychologically. This is a spiritual and metaphysical teaching tradition. Its interpretations can be explored alongside scripture and philosophy, but claims that imagination directly controls external events should not be presented as established scientific findings without independent evidence.",
            "Internet Archive catalogue", "https://archive.org/search?query=Neville+Goddard", "Check rights for each work before redistribution")
    )

    fun search(query: String, limit: Int = 5): List<KnowledgeEntry> {
        val terms = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }.toSet()
        if (terms.isEmpty()) return emptyList()
        return entries.map { entry ->
            val text = (entry.collection + " " + entry.title + " " + entry.body).lowercase()
            val score = terms.sumOf { term ->
                when {
                    entry.title.lowercase().contains(term) -> 5
                    text.contains(term) -> 1
                    else -> 0
                }
            }
            entry to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    fun promptContext(query: String): String {
        val matches = search(query)
        if (matches.isEmpty()) return ""
        return matches.joinToString("\n\n") { entry ->
            "[Library: ${entry.collection} / ${entry.title}]\n${entry.body}\nSource: ${entry.sourceName} — ${entry.sourceUrl}"
        }
    }
}
