<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>summarize</title>
<style>
        /* 파일 업로드 input 요소 스타일링 */
        .custom-file-input {
            display: inline-block;
            position: relative;
            font-size: 11px;
            color: #fff;
            background-color: #007bff;
            border: none;
            border-radius: 5px;
            padding: 8px 20px;
            cursor: pointer;
            outline: none;
            transition: all 0.3s ease;
            box-shadow: 0 1px 5px rgba(0, 0, 0, 0.25);
        }
        
        .custom-file-input:hover {
            background-color: #0056b3;
        }
        
        .custom-file-input input[type="file"] {
            position: absolute;
            left: 0;
            top: 0;
            opacity: 0;
            cursor: pointer;
            width: 100%;
            height: 100%;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }

        fieldset {
            border: none;
            padding: 0;
            margin: 0;
        }

        legend {
            font-weight: bold;
            margin-bottom: 10px;
        }

        textarea {
            width: 100%;
            height: 300px;
            resize: vertical;
            margin-bottom: 10px;
            padding: 8px;
            box-sizing: border-box;
            border-radius: 5px;
            border: 1px solid #ccc;
            transition: border-color 0.3s ease;
            box-shadow: 0 1px 5px rgba(0, 0, 0, 0.25);
        }

        textarea:focus {
            outline: none;
            border-color: grey;
        }

        input[type="submit"], input[type="reset"] {
            padding: 8px 20px;
            border: none;
            border-radius: 5px;
            background-color: #007bff;
            color: #fff;
            cursor: pointer;
            box-shadow: 0 1px 5px rgba(0, 0, 0, 0.25);   
        }

        input[type="submit"]:hover, input[type="reset"]:hover {
            background-color: #0056b3;
        }
    </style>
</head>
<body>
<fieldset>
	<legend>요약 결과</legend>
	
	<form action="save-result.do" method="post" enctype="text">
    
        
        <br>
        <textarea id="summarize" name="summ_result" rows="25" cols="100">${summary_result}</textarea>
        <input type="dir" id="folderInput" webkitdirectory directory multiple>
        <p><input type="submit" value="선택한 폴더에 저장">&nbsp<input type="reset" value="취소"></p>
       
    
	</form>
</fieldset>
</body>
</html>