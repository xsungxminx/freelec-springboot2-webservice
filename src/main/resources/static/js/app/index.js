

var main = {
        init : function(){
            var _this = this;
            $('#btn-save').on('click',function(){
                _this.save();
            });

            $('#btn-update').on('click',function(){ // btn-update란 id를 가진 html 엘리먼트에 click이벤트가 발생할때 update function을 실행하도록 이벤트 등록
                _this.update();
            });

           $('#btn-delete').on('click',function(){ // btn-update란 id를 가진 html 엘리먼트에 click이벤트가 발생할때 update function을 실행하도록 이벤트 등록
                _this.delete();
            });

        },
        save : function (){
            var data = {
                 title: $('#title').val(),
                 author: $('#author').val(),
                 content: $('#content').val()
            };

            $.ajax({
                   type: 'POST',
                   url: '/api/v1/posts',
                   dataType: 'json',
                   contentType:'application/json; charset=utf-8',
                   data: JSON.stringify(data)
               }).done(function() {
                   alert('글이 등록되었습니다.');
                   window.location.href = '/'; // 글 등록이 성공하면 메인페이지(/)로 이동합니다.
               }).fail(function (error) {
                   alert(JSON.stringify(error));
               });
        },
        update : function (){   // update function
            var data ={
                title : $('#title').val(),
                content : $('#content').val()
            }

            var id = $('#id').val();

            $.ajax({
                type: 'PUT',  // 여러 http method 중 put 메소드 선택 postApiController에 있는 API에서 이미 @PutMapping으로 선언했기 때문에 put을 사용해야 합니다. 참고로 이는 REST규약에 맞게 설정된 것
                url: '/api/v1/posts/'+id, // 어느게시글을 수정할지 URL PATH로 구분하기위헤 path에 id를 추가 rest에서 crud는 다음과같이 http method에 매핑 생성(create) post , 읽기(select) get , 수정(update) put , 삭제(delete) delete
                dataType: 'json',
                contentType:'application/json; charset=utf-8',
                data: JSON.stringify(data)
            }).done(function() {
                alert('글이 수정되었습니다.');
                window.location.href = '/';  // 글 수정이 성공하면 메인페이지(/)로 이동합니다.
            }).fail(function (error) {
                alert(JSON.stringify(error));
            });
        },
        delete : function () {
                var id = $('#id').val();

                $.ajax({
                    type: 'DELETE',
                    url: '/api/v1/posts/'+id,
                    dataType: 'json',
                    contentType:'application/json; charset=utf-8'
                }).done(function() {
                    alert('글이 삭제되었습니다.');
                    window.location.href = '/';
                }).fail(function (error) {
                    alert(JSON.stringify(error));
                });
            }
};

main.init();
