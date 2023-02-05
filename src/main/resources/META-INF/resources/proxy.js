const paste = document.getElementById('paste');
const url = document.getElementById('url');
const result = document.getElementById('result')
paste.addEventListener('click', async () => {
    const response = await fetch(`/proxied-url?source-url=${url.value}`)
    const proxyUrl = (await response.json()).proxy_url
    result.value = proxyUrl
    navigator.clipboard.writeText(proxyUrl)
});