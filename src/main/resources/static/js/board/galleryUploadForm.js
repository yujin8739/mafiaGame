// 폼 제출 시 에디터 내용 숨겨진 textarea에 복사
document.querySelector("#galleryUploadForm")
	.addEventListener(
		"submit",
		function(e) {
			document.querySelector("#hiddenDescription").value = quill.root.innerHTML;
		});

// 선택한 파일명 리스트 출력
$('#uploadFile').on('change', function() {
	const files = this.files;
	const $fileList = $('#fileList');
	$fileList.empty();

	if (files.length > 5) {
		alert("사진은 최대 5개까지만 업로드할 수 있습니다.");
		$(this).val(''); // 선택 초기화
		return;
	}

	if (files.length === 0) {
		$fileList.text('선택된 파일이 없습니다.');
		return;
	}

	const list = $('<ul></ul>');
	for (let i = 0; i < files.length; i++) {
		list.append($('<li></li>').text(files[i].name));
	}
	$fileList.append(list);
});