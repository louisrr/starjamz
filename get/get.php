<?php

/**
 * @author Louis Robinson
 * @copyright 2009
 */
include("../includes/cnfg.php");
include("../includes/function.php");
// include("../includes/sitevars.php");
session_start();

$db->connect();



	if ( !empty( $_GET ) )
	{
		$buy = parse_http_args($_GET, array('p'));
		if ($buy['p'])
		{
			$get = (int) escape($buy['p']);
		}
		elseif ($buy['p'] == 0)
		{
			header("Location: http://starjamz.com");
		}
		
		$sql = "SELECT * FROM product, pricepoints WHERE pid=$get AND ( product.price = pricepoints.prc )";
		$rs = $db->query($sql);
		$s = $db->fetch_array($rs);
		$pid = $s['pid'];
		$pp = $s['pricepoint'];
		$mo = $s['keyword_mo_identifier'];
		
		$_SESSION['pid'] = $s['pid'];
		
		?>
		<html>
		<head>
		<title>Get <?php echo strtoupper($s['title']); ?> for your mobile phone!!!!!</title>
		</head>
		<body bgcolor="#75c0dd">		
		<?		
		print "<b>".$s['title']."</b><br />";
		print "<img src='".$s['product_tn']."' width='100' height='100' border='0' />";
		print "<br />".$s['description'];
		print "<br /><b>".$s['pricepoint']."</b>";
		?>	
			<form method="POST" action="http://starjamz.com/get/<?php echo $_SESSION['pid']; ?>">
  				<p>
  					<font face="Arial" size="1">Enter your&nbsp; phone number area code first.</font><br>	
  					
  					<select size="1" name="carrier">
  						<option selected>SELECT Carrier</option>
  						<option value="ALLTEL-13-US">Alltel</option>
  						<option value="ATT-13-US">AT&T</option>
  						<option value="BOOST-13-US">BOOST Mobile</option>
  						<option value="CINBEL-13-US">Cincinnati Bell</option>
  						<option value="CRICKET-13-US">Cricket</option>
  						<option value="DOBSON-13-US">Dobson</option>
  						<option value="NEXTEL-13-US">Nextel</option>
  						<option value="SPRINT-13-US">Sprint</option>
  						<option value="TMOBIL-13-US">T-Mobile</option>
  						<option value="USCELL-13-US">US Cell</option>
  						<option value="VERIZON-13-US">Verizon</option>
  						<option value="VIRGIN-13-US">Virgin Mobile</option>
  					</select>
  					<input type="submit" value="Get it!" name="get">
				</p>
			</form>
		</body>
		</html>
		<?
		if ( !empty( $_POST ) )
		{
			$tr = parse_http_args($_POST, array('carrier'));
			$carrier = escape($tr['carrier']);
			
			$ssl = "SELECT";
			
			if ( $pp != "4.99" && $carrier != "VERIZON-13-US" ) {
				$instr = base64_encode("shortcode=40841&shortcode_color=233,14,91&background_color=0,0,0&keywords=SJ ".$mo."&keyword_color=233,14,91&text_color=205,205,255&cost=$$pp");
			}
			elseif ( $carrier = "VERIZON-13-US" )
			{
				$instr = base64_encode("shortcode=23333&shortcode_color=233,14,91&background_color=0,0,0&keywords=GO ".$mo."&keyword_color=233,14,91&text_color=205,205,255&cost=$$pp");
				
			}elseif ( $pp = "4.99"  ) {
				$instr = base64_encode("shortcode=23333&shortcode_color=233,14,91&background_color=0,0,0&keywords=GO ".$mo."&keyword_color=233,14,91&text_color=205,205,255&cost=$$pp");
			}
			
			?>
				<html>
				<title>Get <?php echo strtoupper($s['title']); ?> for your mobile phone!!!!!</title>
				</head>
				<body>
					<iframe src ="http://us.txtnation.com/shortcode.php?vars=<?= $instr ?>" width="400" height="300" frameborder="0">
  						<p>Your browser does not support iframes.</p>
					</iframe>		
				</body>	
				</html>		
			<?
		}
		
	}
	elseif (empty($_GET))
	{
		header("Location: http://starjamz.com");
	}


   
$db->close();

// call troy's and get receipt

?> 
