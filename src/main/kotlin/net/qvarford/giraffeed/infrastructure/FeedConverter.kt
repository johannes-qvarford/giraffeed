package net.qvarford.giraffeed.infrastructure

import com.google.common.collect.ImmutableBiMap
import net.qvarford.giraffeed.domain.Feed
import net.qvarford.giraffeed.domain.FeedEntry
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.io.StringWriter
import java.net.URI
import java.nio.charset.Charset
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import jakarta.enterprise.context.ApplicationScoped
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilder
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private const val ATOM_NS = "http://www.w3.org/2005/Atom"
private const val DC_NS = "http://purl.org/dc/elements/1.1/"

private val PREFIX_TO_NAMESPACE = ImmutableBiMap.of("atom", ATOM_NS, "dc", DC_NS)
private val NAMESPACE_TO_PREFIX = PREFIX_TO_NAMESPACE.inverse()

// I could use jaxb, but I don't want to get stuck in a situation where 2 feeds have slightly
// different but incompatible representations.
@ApplicationScoped
class FeedConverter(private val builder: DocumentBuilder) {
    init {
        xPath.namespaceContext = object : NamespaceContext {
            override fun getNamespaceURI(prefix: String?): String? {
                return if (!prefix.isNullOrEmpty()) PREFIX_TO_NAMESPACE[prefix] else null
            }

            override fun getPrefix(namespaceURI: String?): String? {
                return if (!namespaceURI.isNullOrEmpty()) NAMESPACE_TO_PREFIX[namespaceURI] else null
            }

            override fun getPrefixes(namespaceURI: String?): MutableIterator<String>? {
                return null
            }
        }
    }

    fun xmlStreamToFeed(stream: InputStream): Feed {
        val document = builder.parse(stream)
        val root = document.documentElement

        if (root.tagName == "rss") {
            root.rssChild("channel").run {
                return Feed(
                    updated = null,
                    icon = rssChild("image").rssText("url").uri(),
                    title = rssText("title"),
                    entries = rssChildren("item")
                        .map { child ->
                            FeedEntry(
                                id = child.rssText("guid"),
                                author = child.rssAuthor(),
                                link = child.rssText("link").uri(),
                                published = child.rssText("pubDate").offsetDateTimeFromAmerican(),
                                title = child.rssText("title"),
                                content = child.rssText("description"),
                                contentType = "html" // TODO: Make this depend on the content
                            )
                        }.toList())
            }

        } else {
            root.run {
                return Feed(
                    updated = atomText("updated").offsetDateTime(),
                    icon = atomText("icon").uri(),
                    title = atomText("title"),
                    entries = atomChildren("entry")
                        .map { child ->
                            FeedEntry(
                                id = child.atomText("id"),
                                author = child.maybeAtomChild("author")?.atomText("name") ?: "/u/[deleted]",
                                link = child.atomLink(null),
                                published = child.atomText("published").offsetDateTime(),
                                title = child.atomText("title"),
                                content = child.atomText("content"),
                                contentType = child.atomAttribute("content", "type")
                            )
                        }.toList()
                )
            }
        }
    }

    fun feedToXmlStream(feed: Feed): InputStream {
        val root = builder.newDocument().run {
            val rootElement = createElementNS(ATOM_NS, "atom:feed")
            appendChild(rootElement)
            DocumentElement(document = this, element = rootElement)
        }

        val str = root.run {
            feed.updated?.let {
                addAtomText("updated", it.format(DateTimeFormatter.ISO_DATE_TIME))
            }

            addAtomText("icon", feed.icon.toString())
            addAtomText("title", feed.title)

            for (entry in feed.entries) {
                val child = root.addAtomElement("entry").apply {
                    val author = addAtomElement("author")
                    author.addAtomText("name", entry.author)
                }

                child.run {
                    addAtomText("id", entry.id)
                    addAtomLink(entry.link.toString())
                    addAtomText("published", entry.published.format(DateTimeFormatter.ISO_DATE_TIME))
                    addAtomText("title", entry.title)
                    addAtomText("content", entry.content, mapOf("type" to entry.contentType))
                }
            }
            documentToString(root.document)
        }

        return str.byteInputStream(Charset.forName("UTF-8"))
    }
}

private fun documentToString(document: Document): String {
    val sw = StringWriter()
    val t: Transformer = TransformerFactory.newInstance().newTransformer()
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    t.setOutputProperty(OutputKeys.INDENT, "yes")
    t.transform(DOMSource(document), StreamResult(sw))
    return sw.toString()
}

val xPath: XPath = XPathFactory.newInstance().newXPath()

private fun Element.atomText(tagName: String): String = atomChildren(tagName).single().textContent

private fun Element.rssText(tagName: String): String = rssChildren(tagName).single().textContent

private fun Element.rssAuthor(): String {
    // this.namespaceChildren("creator", "dc").singleOrNull() doesn't work for some reason, while atom does work.
    // Maybe because the prefix is actually used in the source XML instead of being the default namespace?
    val authors = getElementsByTagNameNS(DC_NS, "creator")
    return authors.item(0).textContent
}


private fun Element.atomAttribute(tagName: String, attribute: String): String =
   atomChildren(tagName)
        .map { it.attributes.getNamedItem(attribute).nodeValue }
        .single()

private fun Element.atomLink(rel: String?): URI {
    return atomChildren("link").filter { it.attributes.getNamedItem("rel")?.nodeValue == rel }
        .map { it.attributes.getNamedItem("href").nodeValue.uri() }
        .single()
}

private fun Element.atomChildren(tagName: String): Sequence<Element> = namespaceChildren(tagName, "atom")

private fun Element.atomChild(tagName: String): Element = atomChildren(tagName).first()

private fun Element.maybeAtomChild(tagName: String): Element? = atomChildren(tagName).firstOrNull()

private fun Element.rssChildren(tagName: String): Sequence<Element> = namespaceChildren(tagName)

private fun Element.rssChild(tagName: String): Element = rssChildren(tagName).first()

private fun Element.namespaceChildren(tagName: String, namespace: String? = null): Sequence<Element> {
    val prefix = if (namespace != null) "$namespace:" else ""
    val expression = "./$prefix$tagName"
    val list = xPath.compile(expression).evaluate(this, XPathConstants.NODESET) as NodeList
    return sequence {
        for (i in 0 until list.length) {
            yield(list.item(i) as Element)
        }
    }.filter { it.tagName == tagName }
}

private data class DocumentElement(val document: Document, val element: Element) {
    fun addAtomText(name: String, value: String, attributes: Map<String, String> = mapOf()): DocumentElement {
        val child = document.createElement("atom:$name")
        for (entry in attributes.entries) {
            child.setAttribute(entry.key, entry.value)
        }
        child.textContent = value
        element.appendChild(child)
        return this
    }

    fun addAtomLink(href: String): DocumentElement {
        val child = document.createElement("atom:link")
        child.setAttribute("href", href)
        element.appendChild(child)
        return this
    }

    fun addAtomElement(name: String): DocumentElement {
        val child = document.createElement("atom:$name")
        element.appendChild(child)
        return DocumentElement(document, child)
    }
}



private fun String.offsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)

private fun String.offsetDateTimeFromAmerican(): OffsetDateTime = OffsetDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME)

private fun String.uri(): URI = URI.create(this)