const quill = new Quill('#editor', { theme: 'snow' });

// 기존 내용 넣기
const rawHtml = detail;
quill.root.innerHTML = decodeHtmlEntities(rawHtml);

function decodeHtmlEntities(str) {
	const textarea = document.createElement('textarea');
	textarea.innerHTML = str;
	return textarea.value;
}