<?php
session_start();
/** *******************************************************************************************
 * @author 						: Louis Robinson											  *
 * @copyright 					: 2009                                                        *
 * @Software              		: starjam                                                     *
 * @Company                     : brxstarjam, inc.                                            * 
 * @Website                     : www.starjamz.com                                            *
 * @Email                       : corporate@starjamz.com                                      *
 * @Technical Support           : (800)681-1283                                               *
 **********************************************************************************************
 *                                                 ,o88888                                    *
 *                                              ,o8888888'                                    *
 *                        ,:o:o:oooo.        ,8O88Pd8888"                                     * 
 *                    ,.::.::o:ooooOoOoO. ,oO8O8Pd888'"                                       *
 *                  ,.:.::o:ooOoOoOO8O8OOo.8OOPd8O8O"                                         *
 *                 , ..:.::o:ooOoOOOO8OOOOo.FdO8O8"                                           *
 *                , ..:.::o:ooOoOO8O888O8O,COCOO"                                             *
 *               , . ..:.::o:ooOoOOOO8OOOOCOCO"                                               *
 *                . ..:.::o:ooOoOoOO8O8OCCCC"o                                                *
 *                   . ..:.::o:ooooOoCoCCC"o:o                                                *
 *                   . ..:.::o:o:,cooooCo"oo:o:                                               *
 *                `   . . ..:.:cocoooo"'o:o:::'                                               *
 *                .`   . ..::ccccoc"'o:o:o:::'                                                *
 *               :.:.    ,c:cccc"':.:.:.:.:.'                                                 *
 *             ..:.:"'`::::c:"'..:.:.:.:.:.'                                                  *
 *           ...:.'.:.::::"'    . . . . .'                                                    * 
 *          .. . ....:."' `   .  . . ''                                                       *
 *        . . . ...."'                                                                        *
 *        .. . ."'     -starjam-                                                              *
 *       .                                                                                    * 
 **********************************************************************************************/
include("../includes/cnfg.php");
include("../includes/function.php");
$db->connect();

/** Initialize Smarty and create new Smarty OBJECT so we can pass everything over to Smarty. */
require($cnfg['Misc']['storepath'].'/Smarty/Smarty.class.php');
$smarty->template_dir = $cnfg['Misc']['storepath'].'templates';
$smarty->compile_dir = $cnfg['Misc']['storepath'].'templates_c';
$smarty->cache_dir = $cnfg['Misc']['storepath'].'Smarty/cache';
$smarty->config_dir = $cnfg['Misc']['storepath'].'Smarty/configs';
$smarty = new Smarty;


	require_once MAIN_DIR . 'fb/src/facebook.php';
	// Create our Application instance.
	$facebook = new Facebook(array(
	  'appId'  => FACEBOOK_APP_ID,
	  'secret' => FACEBOOK_SECRET,
	  'cookie' => true,
	));

	$me = null;
	// Session based API call.
	if ($session) {
	  try {
	    $uid = $facebook->getUser();
	    $me = $facebook->api('/me');
	  } catch (FacebookApiException $e) {
	    error_log($e);
	  }
	}
   
   	if ( login_status() == true ){ $logged_in = true; }else{ $logged_in = false; }
	$twconnect = '<a href="http://starjamz.com/util/oauth/redirect">
			          <img src="http://starjamz.com/img/twitter-signin-short.png" alt="Sign in with Twitter" border="0" />
				      </a>';
	$smarty->assign('logged_in',$logged_in);
	$smarty->assign('connect',$connect);
	$smarty->assign('json_session',json_encode($session));
	$smarty->assign('appid',FACEBOOK_APP_ID);		
	
	$loginUrl = '<a href="'.$facebook->getLoginUrl().'"  >
	  			 	<img id="fb_login_image" src="http://starjamz.com/img/fb-connect-small.png"  alt="Connect" border="0" / >
				 </a>';
	
	$twconnect = '<a href="http://starjamz.com/util/oauth/redirect">
			          <img src="http://starjamz.com/img/twitter-signin-short.png" alt="Sign in with Twitter" border="0" />
				      </a>';	
	$smarty->assign('loginUrl',$loginUrl);	
	$smarty->assign('twconnect',$twconnect);
	$logoutUrl = $facebook->getLogoutUrl();

	
/** Assign the current time into a variable*/	
$now = date("d-m-Y H:i:s", time());
$time = strtotime($now);	
$today = strtotime( date ("L F Y at h:i" ) ); 

// Create our Application instance.
$facebook = new Facebook(array(
  'appId'  => FACEBOOK_APP_ID,
  'secret' => FACEBOOK_SECRET,
  'cookie' => true,
));

$me = null;
// Session based API call.
if ($session) {
  try {
    $uid = $facebook->getUser();
    $me = $facebook->api('/me');
  } catch (FacebookApiException $e) {
    error_log($e);
  }
}

// Login URL
$loginUrl = $facebook->getLoginUrl();
$logoutUrl = $facebook->getLogoutUrl();

if ( !empty ( $_GET ) ) {
	$uu = parse_http_args($_GET,array('p'));
	$pid = (int)escape( no_special_chars( $uu['p'] ) );
	$smarty->assign('pid',$pid);
	if ( $pid > 0 ) {
		$ss = "SELECT pid, approved FROM product WHERE pid=$pid";
		$ro = $db->squeryf($ss);
		$fo = $db->fetch_array($ro);
		if ( $fo['approved'] == 'yes') {
			$template = "item";
		} elseif ( $fo['approved']='no' ) {
			$template = "item_not_approved";
			$reason = "The item: &quot;<span class='err'>".$pid."</span>&quot; is not yet approved.";
			$smarty->assign('reason',$reason);
		}
	} elseif ( empty ( $fo['pid'] ) ) {
		$template = "item_fail";
	}  /** elseif ( $fo['begin_date'] > $now ) {
		$template = "item_not_approved";
		$te = 1;
		$reason = "The item: &quot;<span class='err'>".$fo['pid']."</span>&quot; is not yet available.";
		$smarty->assign('reason',$reason);
	} elseif ( $fo['end_date'] < $now ) {
		$template = "item_not_approved";
		$tl = 1;
		$reason = "The item: &quot;<span class='err'>".$fo['pid']."</span>&quot; is no longer available.";
		$smarty->assign('reason',$reason);
	} */
	
	if  ( !empty ( $fo['pid'] ) AND $fo['approved']='yes'/*AND $te != 1 AND  $tl != 1 */ ) {
		
		$p = $fo['pid'];
		
		$sql = "SELECT pid, aid, title, cid, type, description, price, product_preview, product_tn, copyrightholder, 
				DATE_FORMAT( date_added, '%W %M %Y at %H:%i' ) AS Date, downloads, views, stock_type, sc, 
				allowed_networks, meta_description, tags, compatible_devices, device_match_id, keyword_mo_identifier
				FROM product WHERE pid=$p";
		$rs = $db->squeryf($sql);
		$product = $db->fetch_array($rs);
		
		if ( $product['aid'] != NULL AND $product['aid']) {
			$cnn = $db->query("SELECT sname FROM artist WHERE aid=".$product['aid']);
			$a = $db->fetch_array($cnn);
			$artist = $a['sname'];
		}
		
		if ( $product['cid'] != NULL AND $product['cid'] > 0 ) {
			$pls = $db->query("SELECT cat_name FROM categories WHERE cid=".$product['cid']);
			$cc = $db->fetch_array($pls);
			$type = $cc['cat_name']; // category
		}
		
		if ( $product['copyrightholder'] != NULL AND $product['copyrightholder'] > 0 ) {
			$hln = $db->query("SELECT * FROM copyrightholder WHERE crhno=".$product['copyrightholder']);
			$gt = $db->fetch_array($hln);
			$co = $gt['companyname'];
		}
		
		if ( $product['type'] != NULL AND $product['type'] > 0 ) {
			$pt = $db->query("SELECT ptname FROM product_type WHERE pt=".$product['type']);
			$tpe = $db->fetch_array($pt);
			$genre = $tpe['ptname']; // genre
		}
		
		if ( $product['price'] != NULL AND $product['price'] > 0 ) 
		{
			$atm = $db->squeryf("SELECT pricepoint, credits FROM pricepoints WHERE prc=".$product['price']);
			$cash = $db->fetch_array($atm);
			$price = $cash['pricepoint']; // price in dollars
			$credits = $cash['credits']; // credits
		}
		
		// Data listed and declared as variables.
		$pid = $product['pid'];
		$cid = $product['cid'];
		$title = $product['title'];
		$added = $product['Date'];
		$tags = $product['tags'];
		
		
		if ( $product['product_tn'] != '' )
		{
			/** */
			$thumb = $product['product_tn'];
		} else {
			$thumb = 'http://starjamz.com/img/sub/default_gr_icon.png';
		}
		
		$preview = $product['product_preview'];
		$tags = $product['tags'];
	
		$shdesc = substr($product['description'], 0, 220);
		   $desclen = strlen($product['description']);
		$longdesc = substr($product['description'], 221, $desclen);
		$shcompdv = substr($product['compatible_devices'], 0, 220);
		       $cdlen = strlen($product['compatible_devices']);
		$lncompdv = substr($product['compatible_devices'], 221, $cdlen);

		if ($cid = 1)
		{
			$heading = "STARJAMZ - ".$producer." &quot;".$title."&quot; ".$type;
		}
    	elseif ($cid = 2)
    	{
    		$heading = "STARJAMZ - &quot;".$title." &quot;  ".$type."";
    	}
    	elseif ($cid = 3)
   		{
    		$heading = "STARJAMZ - &quot;".$title."&quot;";
    	}
    	elseif ($cid = 4)
    	{
    		$heading = "STARJAMZ - &quot;".$title."&quot; ".$type."";
    	}
    	elseif ($cid = 5)
    	{	
        	$heading = "STARJAMZ - &quot;".$title."&quot; ".$type."";
    	}
    	elseif ($cid = 6)
    	{
    		$heading = "STARJAMZ - &quot;".$title."&quot; ".$type."";
    	}
    	elseif ($cid = 7)
    	{
			$heading = "STARJAMZ - &quot;".$title."&quot; AS SEEN ON TV!!!!";
    	}
		$smarty->assign('heading',$heading);

		/* Use an if/else case to determine if a given item will be assigned an artist or producer based upon category.
		if ($product['cid']=1) { $producer = $product['sname']; }
		else { $producer = $product['companyname']; }	*/
		$cid = $product['cid'];
		$smarty->assign('desc',substr($product['description'], 0, 150));
		$smarty->assign('meta_keywords',substr($product['tags'], 0, 150));
		$smarty->assign('pid',$pid);
		$smarty->assign('url',URL . '/go/' . $pid);
		$smarty->assign('genre',$genre);	
		$smarty->assign('artist',$artist);
		$smarty->assign('co',$co);
		$smarty->assign('pricepoint',$pricepoint);
		$smarty->assign('credits',$credits);
		$smarty->assign('cid',$cid);
		$smarty->assign('title',$title);
		$smarty->assign('added',$added);
		$smarty->assign('smlr_one',$added);
		$smarty->assign('thumb',$thumb);
		$smarty->assign('producer',$producer);
		$smarty->assign('tags',$tags);
		$smarty->assign('type',$type);
		$smarty->assign('thumb',$thumb);
		$smarty->assign('preview',$preview);
		$smarty->assign('shdesc',$shdesc);
		$smarty->assign('longdesc',$longdesc);
		$smarty->assign('shcompdv',$shcompdv);
		$smarty->assign('lncompdv',$lncompdv);
		
		$smarty->assign('twconnect',$twconnect);

		$sY = "SELECT pid, title, product_tn, cid, tags  
		       FROM product 
			   WHERE cid=$cid 
			   AND approved='yes' 
			   AND MATCH (tags) 
       		   AGAINST ('+$tags' IN BOOLEAN MODE) 
			   LIMIT 15";
		$rY = $db->query($sY);
		if ( !$rY ) {

			$sU = "SELECT views, product_tn, pid, cid, tags FROM product WHERE cid=$cid AND approved='yes' AND MATCH (tags) 
       		       AGAINST ('+$tags' IN BOOLEAN MODE)
       		       ORDER BY views LIMIT 15";
			$rU = $db->query($sU);
 			$i=0;
			while ( $fU = $db->fetch_array($rU) ) {
					$i++;
					if ($i < 6)
					{
						$smlr_one[] = $fU;
					}
					if ($i > 5 AND $i < 11)
					{
						$smlr_two[] = $fU;
					}
					if ($i > 10 AND $i < 16)
					{
						$smlr_three[] = $fU;
					}
			}		
	} elseif ( $fY['downloads'] > 0 AND $fY['downloads'] < 15 ) {
			$remainder = 15 - count($fY);
			if ( $remainder > 15 ) {
				$remainder = 15;
			}
			$sU = "SELECT views, product_tn, pid, cid, tags FROM product WHERE cid=$cid AND approved='yes'
				   AND MATCH (tags) 
       			   AGAINST ('+$tags' IN BOOLEAN MODE)
       			   LIMIT $remainder";
			$rU = $db->query($sU);
			$fU = $db->fetch_array($rU);
			$popularity = array_merge($fY, $fU);
			$i=0;
			foreach ( $popularity AS $p ) {
				$i++;
					if ($i < 6)
					{
						$smlr_one[] = $p;
					}
					if ($i > 5 AND $i < 11)
					{
						$smlr_two[] = $p;
					}
					if ($i > 10 AND $i < 16)
					{
						$smlr_three[] = $p;
					}
			}
 
		} else {
			$i=0;
			while ( $fY = $db->fetch_array($rY) ) {
			$i++;
					if ($i < 6)
					{
						$smlr_one[] = $fY;
					}
					if ($i > 5 AND $i < 11)
					{
						$smlr_two[] = $fY;
					}
					if ($i > 10 AND $i < 16)
					{
						$smlr_three[] = $fY;
					}
			}
		}
		
		$smarty->assign('p',$p);
		$smarty->assign('pid',$pid);
		$smarty->assign('smlr_one',$smlr_one);
		$smarty->assign('smlr_two',$smlr_two);
		$smarty->assign('smlr_three',$smlr_three);


		// Get a 468x60 Advertisement
		$ads = "SELECT * FROM `ads` WHERE size='468x60' AND adid >= 
				(SELECT FLOOR( MAX(adid) * RAND()) FROM `ads`) ORDER BY adid LIMIT 1";
		$a = $db->query($ads);
		$advert = $db->fetch_array($a);
		$adwide = $advert['embed_code'];
		$smarty->assign('adwide',$adwide);	
	
		// Get a 300x299 Advertisement
		$ads = "SELECT * FROM `ads` WHERE size='300x250' AND adid >= 
			    (SELECT FLOOR( MAX(adid) * RAND()) FROM `ads`) ORDER BY adid LIMIT 1";
		$ad_sql = $db->query($ads);
		$ad = $db->fetch_array($ad_sql);
		$adv = $ad['embed_code'];
		$smarty->assign('adv',$adv);

		$dn = "SELECT views AS popularity, 
			   pid, title FROM product WHERE approved='yes' ORDER BY popularity LIMIT 10";
    	$tSQL = $db->query($dn);
		while ($tp = $db->fetch_array($tSQL))
		{
			$top[] = $tp;
		}
		$smarty->assign('top',$top);

			/** Retrieve the amount of views this product has
			$vw = "SELECT views FROM product WHERE pid=$pid";
			$dat = $db->squeryf($vw);
			$iR =   $db->fetch_array($dat);*/
			$views = $product['views'];
			$views++;
	
			if ( $pid != NULL AND $pid > 0 ) {
				$yii = "UPDATE product SET views=$views WHERE pid=$pid";
				$cntd =  $db->query($yii);	
			}
		
			/** Log this page view as an instance of product_exposure */
			productExposure($pid, $cid, 1);
			LogPageView();		
		}

	// Handle the rest of the Smarty variables..
	$smarty->assign('err',$err);	
	$smarty->assign('msg',$msg);
	$smarty->display($cnfg['Misc']['storepath']. "templates/".$template.".tpl");

}
elseif ( empty ( $_GET ) ) 
{
	header("Location: ".$cnfg['site']['url']);
}

$db->close();
