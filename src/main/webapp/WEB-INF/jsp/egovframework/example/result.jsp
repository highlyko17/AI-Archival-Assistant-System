<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="UTF-8">
		<title>ARKEEPER Result</title>
		<link rel="stylesheet" href="sample.css">
		<!-- Linking the CSS file -->
			<style>
				img.title {
				  display: block; /* 이미지를 블록 레벨 요소로 설정 */
				  margin: 0 auto; /* 가로 방향 마진을 auto로 설정하여 가운데 정렬 */
				  padding: 20px;
				  max-width: 100%; /* 이미지 너비가 부모 요소를 초과하지 않도록 설정 */
				  height: auto; /* 이미지 높이를 자동으로 설정하여 비율 유지 */
				  margin-bottom:-40px;
				}
				body {
					font-family: font-family : -apple-system, BlinkMacSystemFont, sans-serif;
					margin: 0;
					padding: 0;
					background-color: #f4f4f4;
				}
				
				form {
					width: 60%;
					margin-left: 10%; /* 왼쪽 여백 추가 */
					margin-right: 10%; /* 오른쪽 여백 추가 */
					background-color: #fff;
					padding-left: 30px;
					border-radius: 5px;
					box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
				}
				
				h3 {
					font-size: 24px;
					color: gray;
					padding: 20px;
					width: 80%; /* 전체 너비의 80%로 설정하여 좌우 여백을 동일하게 만듦 */
					margin: 10px auto; /* 위아래 여백 10px, 좌우 여백 자동 */
					text-align: center; /* 텍스트를 가운데 정렬 */
				}
				
				.summary-container {
					width: 60%;
					margin: 20px auto;
					background-color: #ffffff;
					padding: 20px;
					border-radius: 5px;
					box-shadow: 0 2px 5px rgba(0, 0, 0, .25);
					/* Applying the same box-shadow effect */
				}
				
				p {
					font-size: 16px; /* 텍스트 크기 */
					color: #333; /* 텍스트 색상 */
					width: 80%; /* 가로 길이 지정 */
					max-width: 600px; /* 최대 가로 길이 지정 */
					margin: 0 auto; /* 가운데 정렬을 위한 margin 설정 */
					padding: 10px; /* 내부 여백 설정 */
					line-height: 1.6; /* 텍스트 줄 간격 설정 */
				}
			</style>
	</head>
	<body>
		<img class="title" src="/images/egovframework/arkeeperlogo/FullLogo_Transparent.png" alt="Description of the image" width="320" height="256">
		<div class="summary-container">
			<p>${arkeeperresult}</p>
		</div>
	</body>
</html>
