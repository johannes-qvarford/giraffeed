// TODO: Verify before showing result
const expectedState = localStorage.getItem("state");
const result = document.getElementById('result');

const hash = new URLSearchParams(location.hash.substring(1));
const accessToken = hash.get("access_token");
const actualState = hash.get("state");

if (expectedState != actualState) {
    result.value = "Request was not initiated by this browser"
} else {
    result.value = hash.get("access_token");
}
