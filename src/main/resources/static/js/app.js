const pwd = document.getElementById("password");
const chk = document.getElementById("check-pwd");

chk.onchange = function (e) {
    pwd.type = chk.checked ? "text" : "password";
}