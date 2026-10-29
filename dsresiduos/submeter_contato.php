<?php

if ($_SERVER['REQUEST_METHOD'] == 'POST') {

//check if its an ajax request, exit if not
if (!isset($_SERVER['HTTP_X_REQUESTED_WITH']) AND strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) != 'xmlhttprequest') {

		//exit script outputting json data
		$output = json_encode(
						array(
								'type' => 'error',
								'text' => 'Request must come from Ajax'
		));

		die($output);
}

//check $_POST vars are set, exit if any missing
if (!isset($_POST["nome"]) || !isset($_POST["mensagem"])) {
		$output = json_encode(array('type' => 'error', 'text' => 'Input fields are empty!'));
		die($output);
}

//Sanitize input data using PHP filter_var().
$nome = filter_var(trim($_POST["nome"]), FILTER_SANITIZE_STRING);
$email = filter_var(trim($_POST["email"]), FILTER_SANITIZE_EMAIL);
$mensagem = filter_var(trim($_POST["mensagem"]), FILTER_SANITIZE_STRING);
$telefone = filter_var(trim($_POST["telefone"]), FILTER_SANITIZE_STRING);

//additional php validation
//if (strlen($nome) < 4) { // If length is less than 4 it will throw an HTTP error.
//		$output = json_encode(array('type' => 'error', 'text' => 'Name is too short!'));
//		die($output);
//}
// if (!filter_var($useremail, FILTER_VALIDATE_EMAIL)) { //email validation
// 		$output = json_encode(array('type' => 'error', 'text' => 'Please enter a valid email!'));
// 		die($output);
// }
//if (strlen($mensagem) < 5) { //check emtpy message
//		$output = json_encode(array('type' => 'error', 'text' => 'Too short message!'));
//		die($output);
//}
$output = json_encode(array('type' => 'message', 'text' => 'Hi ' . $nome . 'Agradecemos seu contato!!'));
//die($output);



// //API Url
$url = 'http://176.223.140.239:8080/gestaofacil/rest/service/inserirMensagemSite';

//Initiate cURL.
$ch = curl_init($url);

//The JSON data.
$jsonData = array(
	 'nome' => $nome,
	 'email' => $email,
	 'telefones' => $telefone,
	 'mensagem' => $mensagem
);

//Encode the array into JSON.
$jsonDataEncoded = json_encode($jsonData);

//Tell cURL that we want to send a POST request.
curl_setopt($ch, CURLOPT_POST, 1);

//Attach our encoded JSON string to the POST fields.
curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonDataEncoded);

//Returns TRUE on success or FALSE on failure. However, if the CURLOPT_RETURNTRANSFER option is set, it will return the result on success, FALSE on failure.
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);

//Set the content type to application/json
curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));

//Execute the request
$result = curl_exec($ch);
//echo $result.situacao;

//if($result == false) {
$obj = json_decode($result);
//if ($obj->{'situacao'} == 'feito') {
if (strpos($result, 'situacao') == true){
$output = json_encode(array('type' => 'message', 'text' => 'Olá, ' . $nome . ' retornaremos em breve!!'));
		die($output);
  //  die(curl_error($ch));
}else{
	$output = json_encode(array('type' => 'error', 'text' => 'Algo deu errado no envio da mensagem, entre em contato por telefone ou whatsapp. Pedimos desculpas pelo transtorno!!'));

	die($output);
}


// $to = "info@wearecoders.net"; //Replace with recipient email address
// //proceed with PHP email.
// $headers = 'From: ' . $useremail . '' . "\r\n" .
// 				'Reply-To: ' . $useremail . '' . "\r\n" .
// 				'X-Mailer: PHP/' . phpversion();
//
// $sentMail = @mail($to, $subject, $message . '  -' . $username, $headers);
// //$sentMail = true;
// if (!$sentMail) {
// 		$output = json_encode(array('type' => 'error', 'text' => 'Could not send mail! Please contact administrator.'));
// 		die($output);
// } else {
// 		$output = json_encode(array('type' => 'message', 'text' => 'Hi ' . $username . ' Thank you for your email'));
// 		die($output);
// }
}
?>

</body>

</html>
