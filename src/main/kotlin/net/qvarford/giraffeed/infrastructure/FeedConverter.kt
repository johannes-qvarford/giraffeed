package net.qvarford.giraffeed.infrastructure

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
import javax.enterprise.context.ApplicationScoped
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


// I could use jaxb, but I don't want to get stuck in a situation where 2 feeds have slightly
// different but incompatible representations.
@ApplicationScoped
class FeedConverter(private val builder: DocumentBuilder) {
    init {
        xPath.namespaceContext = object : NamespaceContext {
            override fun getNamespaceURI(prefix: String?): String? {
                return if (prefix == "atom") { ATOM_NS } else { null }
            }

            override fun getPrefix(namespaceURI: String?): String? {
                return if (namespaceURI == ATOM_NS) { return "atom" } else { null }
            }

            override fun getPrefixes(namespaceURI: String?): MutableIterator<String>? {
                return null;
            }
        }
    }

    fun xmlStreamToFeed(stream: InputStream): Feed {
        val document = builder.parse(stream)
        val root = document.documentElement;

        if (root.tagName == "rss") {
            val channel = root.rssChild("channel");
            return Feed(
                updated = null,
                icon = channel.rssChild("image").rssText("url").uri(),
                title = channel.rssText("title"),
                entries = channel.rssChildren("item")
                    .map { child ->
                        FeedEntry(
                            id = child.rssText("guid"),
                            link = child.rssText("link").uri(),
                            published = child.rssText("pubDate").offsetDateTime(),
                            title = child.rssText("title"),
                            content = child.rssText("description"),
                            contentType = "html" // TODO: Make this depend on the content
                        )
                    }.toList()
            )
        } else {
            return Feed(
                updated = root.atomText("updated").offsetDateTime(),
                icon = root.atomText("icon").uri(),
                title = root.atomText("title"),
                entries = root.atomChildren("entry")
                    .map { child ->
                        FeedEntry(
                            id = child.atomText("id"),
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

    fun feedToXmlStream(feed: Feed): InputStream {
        val document = builder.newDocument()

        val rootElement = document.createElementNS(ATOM_NS, "atom:feed")
        document.appendChild(rootElement)

        val root = DocumentElement(document = document, element = rootElement)

        if (feed.updated != null) {
            root.addAtomText("updated", feed.updated.format(DateTimeFormatter.ISO_DATE_TIME))
        }
        root.addAtomText("icon", feed.icon.toString())
        root.addAtomText("title", feed.title)

        for (entry in feed.entries) {
            val child = root.addAtomElement("entry")
            child.addAtomText("id", entry.id)
            child.addAtomLink(entry.link.toString())
            child.addAtomText("published", entry.published.format(DateTimeFormatter.ISO_DATE_TIME))
            child.addAtomText("title", entry.title)
            child.addAtomText("content", entry.content, mapOf("type" to entry.contentType))
        }

        val str = documentToString(document)
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

private fun Element.atomText(tagName: String): String = this.atomChildren(tagName).single().textContent

private fun Element.rssText(tagName: String): String = this.rssChildren(tagName).single().textContent

private fun Element.atomAttribute(tagName: String, attribute: String): String =
    this.atomChildren(tagName)
        .map { it.attributes.getNamedItem(attribute).nodeValue }
        .single()

private fun Element.atomLink(rel: String?): URI {
    return this.atomChildren("link").filter { it.attributes.getNamedItem("rel")?.nodeValue == rel }
        .map { it.attributes.getNamedItem("href").nodeValue.uri() }
        .single()
}

private fun Element.atomChildren(tagName: String): Sequence<Element> {
    val expression = "./atom:$tagName"
    val list = xPath.compile(expression).evaluate(this, XPathConstants.NODESET) as NodeList
    return sequence {
        for (i in 0 until list.length) {
            yield(list.item(i) as Element)
        }
    }.filter { it.tagName == tagName }
}

private fun Element.rssChildren(tagName: String): Sequence<Element> {
    val expression = "./$tagName"
    val list = xPath.compile(expression).evaluate(this, XPathConstants.NODESET) as NodeList
    return sequence {
        for (i in 0 until list.length) {
            yield(list.item(i) as Element)
        }
    }.filter { it.tagName == tagName }
}

private fun Element.rssChild(tagName: String): Element {
    return this.rssChildren(tagName).first()
}

private data class DocumentElement(val document: Document, val element: Element) {
    fun addAtomText(name: String, value: String, attributes: Map<String, String> = mapOf()): DocumentElement {
        val child = document.createElement("atom:$name");
        for (entry in attributes.entries) {
            child.setAttribute(entry.key, entry.value)
        }
        child.textContent = value
        element.appendChild(child)
        return this
    }

    fun addAtomLink(href: String): DocumentElement {
        val child = document.createElement("atom:link");
        child.setAttribute("href", href)
        element.appendChild(child)
        return this
    }

    fun addAtomElement(name: String): DocumentElement {
        val child = document.createElement("atom:$name");
        element.appendChild(child)
        return DocumentElement(document, child)
    }
}



private fun String.offsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)

private fun String.uri(): URI = URI.create(this)

private val ATOM_NS: String = "http://www.w3.org/2005/Atom"