const toolbarOptions = [
	['bold', 'italic', 'underline'],
	[{ 'list': 'ordered' }, { 'list': 'bullet' }],
	[{ 'header': [1, 2, false] }],
	['link', 'image']
];

const quill = new Quill('#editor', {
	modules: { toolbar: toolbarOptions },
	placeholder: '내용을 입력하세요...',
	theme: 'snow'
});