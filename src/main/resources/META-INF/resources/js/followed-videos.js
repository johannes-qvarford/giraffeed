const state = Math.random();
localStorage.setItem("state", state);

const submit = document.getElementById('submit');

submit.addEventListener('click', async () => {
    location.href = `https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=933nb4cfbbv6rws5wo0yr2w7mjdn4g&redirect_uri=${location.protocol}//${location.host}/auth/callback&scope=user:read:follows&state=${state}`
});