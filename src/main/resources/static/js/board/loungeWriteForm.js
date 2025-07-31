
document.querySelector("form").addEventListener("submit", function() {
	document.querySelector("#hiddenContent").value = quill.root.innerHTML;
});

