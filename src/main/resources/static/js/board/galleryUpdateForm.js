let deletedFiles = [];

// 이미지 제거 버튼 클릭 시
$(document).on('click', '.remove-btn', function() {
	const fileId = $(this).data('id');
	$('#img-box-' + fileId).remove();
	$('input[name="remainFileList"][value="' + fileId + '"]').remove();

	deletedFiles.push(fileId);
	$('#deletedFileList').val(deletedFiles.join(','));
});

//새 파일명 목록 표시
$('#newFiles').on('change', function() {
	const fileList = Array.from(this.files);
	const display = fileList.map(file => `<div>• ${file.name}</div>`).join('');
	$('#newFileNameList').html(display);
});

// 제출 시 검사
$('#galleryForm').on('submit', function(e) {
	const remainImages = $('input[name="remainFileList"]').length;
	const newImages = $('#newFiles')[0].files.length;

	if (remainImages + newImages === 0) {
		alert('최소한 한 장 이상의 이미지를 포함해야 합니다.');
		e.preventDefault();
		return false;
	} else if (remainImages + newImages > 5) {
		alert('사진은 최대 5개까지만 업로드할 수 있습니다.');
		e.preventDefault();
		return false;
	}

	$('#detailInput').val(quill.root.innerHTML);
});