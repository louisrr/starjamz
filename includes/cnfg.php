<?php
 
/*********************************************************************************************************
*                                                                                                        *
*  |   Software                     : starjam                                                            *
*                                                                                                        *
*  |   Software Author              : brxstarjam, inc.                                                   * 
*                                                                                                        *
*  |   Website                      : www.starjamz.com                                                   *
*                                                                                                        *
*  |   E-mail                       : corporate@starjamz.com                                             *
*                                                                                                        *
*  |   Technical Support            : (800)681-1283                                                      *
*                                                                                                        *
*                                                                                                        *
**********************************************************************************************************
* This source file is subject to the rules and regulations of BRXSTARJAM, Incorporated and authorized    *
* only for company use. If you so happen to obtain this file please destroy it and notify the technical  *
* administration of brxstarjam, inc. at (800)681-1283 or the corporate office by e-mailing:              *
* corporate@starjamz.com. Any use of this source file by un-authorized users will result in legal action.*
**********************************************************************************************************
*                                                                                                        *
*                                                                                                        *
*                                                 ,o88888                                                *
*                                              ,o8888888'                                                *
*                        ,:o:o:oooo.        ,8O88Pd8888"                                                 * 
*                    ,.::.::o:ooooOoOoO. ,oO8O8Pd888'"                                                   *
*                  ,.:.::o:ooOoOoOO8O8OOo.8OOPd8O8O"                                                     *
*                 , ..:.::o:ooOoOOOO8OOOOo.FdO8O8"                                                       *
*                , ..:.::o:ooOoOO8O888O8O,COCOO"                                                         *
*               , . ..:.::o:ooOoOOOO8OOOOCOCO"                                                           *
*                . ..:.::o:ooOoOoOO8O8OCCCC"o                                                            *
*                   . ..:.::o:ooooOoCoCCC"o:o                                                            *
*                   . ..:.::o:o:,cooooCo"oo:o:                                                           *
*                `   . . ..:.:cocoooo"'o:o:::'                                                           *
*                .`   . ..::ccccoc"'o:o:o:::'                                                            *
*               :.:.    ,c:cccc"':.:.:.:.:.'                                                             *
*             ..:.:"'`::::c:"'..:.:.:.:.:.'                                                              *
*           ...:.'.:.::::"'    . . . . .'                                                                * 
*          .. . ....:."' `   .  . . ''                                                                   *
*        . . . ...."'                                                                                    *
*        .. . ."'     -starjam-                                                                          *
*       .                                                                                                * 
*                                                                                                        *
*                                                                                                        *
**********************************************************************************************************/



// ======================= CONFIGURATION ================================


//	****** DATABASE TYPE ******
// This is the type of the database server on which your gamastore database will be located.
// Currently the only supported database is mysql.
// for slave options just append _slave to your preferred database type.
$cnfg['Database']['dbtype'] = 'mysql';

// ****** TABLE PREFIX ******
// Prefix that your vBulletin tables have in the database.
$cnfg['Database']['tableprefix'] = '';

// Production Login Credentials
define (PRODUCTION_SERVER,'localhost');
define (PRODUCTION_USERNAME,'[username]');
define (PRODUCTION_PASSWORD, '[password]');
define (PRODUCTION_DATABASE, '[database]');
define (PRODUCTION_PREFIX, '');




// ****** TECHNICAL EMAIL ADDRESS ******
// If any database errors occur, they will be emailed to the address specified here.
// Leave this blank to not send any emails when there is a database error.
$cnfg['Database']['technicalemail'] = 'support@starjamz.com';

//	****** FORCE EMPTY SQL MODE ******
// New versions of MySQL (4.1+) have introduced some behaviors that are
// incompatible with gamastore. Setting this value to "true" disables those
// behaviors. You only need to modify this value if vBulletin recommends it.
$cnfg['Database']['force_sql_mode'] = false;

// ****** MASTER DATABASE SERVER NAME AND PORT ******
// This is the hostname or IP address and port of the database server.
// If you are unsure of what to put here, leave the default values.
$cnfg['MasterServer']['servername'] = 'localhost';
$cnfg['MasterServer']['port'] = 3306;

// ****** MASTER DATABASE PERSISTENT CONNECTIONS ******
// This option allows you to turn persistent connections to MySQL on or off.
// The difference in performance is negligible for all but the largest boards.
// If you are unsure what this should be, leave it off. (0 = off; 1 = on)
$cnfg['MasterServer']['usepconnect'] = 0;


// ****** PATH TO ADMIN & MODERATOR CONTROL PANELS ******
// This setting allows you to change the name of the folders that the admin and
// moderator control panels reside in. You may wish to do this for security purposes.
// Please note that if you change the name of the directory here, you will still need
// to manually change the name of the directory on the server.
$cnfg['Misc']['admincpdir'] = 'centralprime';
$cnfg['Misc']['modcpdir'] = 'control';

// Prefix that all vBulletin cookies will have
// Keep this short and only use numbers and letters, i.e. 1-9 and a-Z
$cnfg['Misc']['cookieprefix'] = 'sj';

// ******** FULL PATH TO Website public_html DIRECTORY ******
// Do not include a trailing slash!
// Example Unix:
// $config['Misc']['forumpath'] = '/home/users/public_html/gama';
// Example Win32:
// $config['Misc']['forumpath'] = 'c:\program files\apache group\apache\htdocs\gm';
$cnfg['Misc']['storepath'] = '/home/starjam/public_html/';
define(MAIN_DIR,'/home/starjam/public_html/');

// Path to Function file or function.php
define (GENERAL_FUNCTIONS, '/home/starjam/public_html/includes/function.php');

// Path to Facebook Graph API / PHP SDK / Oauth 2.0
define (FACEBOOK, '/home/starjam/public_html/fb/src/facebook.php');

// Path to Twitter Oauth
define (TWITTER, '/home/starjam/public_html/util/oauth/twitteroauth/twitteroauth.php');


/**
 * The paths defined below will be for application-specific directories advertise, admincp, cgi-bin, class, css, 
 * facebook, twitter_oauth, flash/swf, fonts, sharing i.e. get, product i.e. go, img, includes, js, lib, mediabox,
 * mobilelib, model, music, publisher, settings, Smarty, templates, templates_c, twit, util, i.e. yx3m, java game 
 * dir: i.e. yxr 
 */
define(AD,'/home/starjam/public_html/advertise/'); // Path to advertise dir
define(ADMINCP,'/home/starjam/public_html/centralprime/'); // Administation CP Directory
define(CGI,'/home/starjam/public_html/cgi-bin/'); // Path to Cgi-bin
define(CLASSES,'/home/starjam/public_html/class/'); // Path to directory for classes
define(CSS,'/home/starjam/public_html/css/'); // Path to directory for all .CSS files
define(FLASH_DIR,'/home/starjam/public_html/fl/'); // Path to Directory for flash; .fla, .swf files
define(FONT,'/home/starjam/public_html/font/'); // Path to Directory for font
define(GET,'/home/starjam/public_html/get/'); // Dir for snw & sharing files
define(PRODUCTS,'/home/starjam/public_html/go/'); // Path to directory for product display files
define(IMAGES,'/home/starjam/public_html/img/'); // Path to directory for images
define(INCLUDES,'/home/starjam/public_html/include/'); // Path to directory for includes
define(JS,'/home/starjam/public_html/js/'); // Path to directory for javascript
define(LIBRARY,'/home/starjam/public_html/lib/'); // Path to code library
define(MEDIABOX_DIR,'/home/starjam/public_html/mediabox/'); // Path to directory for mediabox
define(MOBILE,'/home/starjam/public_html/mobilelib/'); // Directory for mobile functions
define(TALENT,'/home/starjam/public_html/model/'); // Path to model submission form directory
define(USER_SETTINGS_DIR,'/home/starjam/public_html/settings/'); // Path to user settings directory
define(SMARTY,'/home/starjam/public_html/smarty/'); // Path to Smarty directory
define(SMARTY_TEMPLATES,'/home/starjam/public_html/templates/'); // Path to template directory
define(SMARTY_COMPILED,'/home/starjam/public_html/templates_c/'); // Path to template_c
define(UTILITIES,'/home/starjam/public_html/util/'); // Path to utilities
define(APP_DIR,'/home/starjam/public_html/yxr/'); // Path to mobile games directory 
define(OFC_DIR,'/home/starjam/public_html/class/ofc/'); // NO TRAILING SLASHES WHATSOEVER


//The actual web address for this website
$cnfg['site']['url'] = 'http://starjamz.com/';
define(URL,'http://starjamz.com');

//The actual NAME of this website
$cnfg['site']['name'] = 'Starjamz&trade;';
define(SITE_NAME,'Starjamz&trade;');

// NO REPLY E-mail address
define(NO_REPLY,'no-reply@starjamz.com');

// ******** SPECIAL USERS WHO CAN RUN QUERRIES ******** 
// The users specified here will be allowed to run queries from the control panel.
// See the above entries for more information on the format.
// Please note that the ability to run queries is quite powerful. You may wish
// to remove all user IDs from this list for security reasons.
define(CAN_RUN_QUERIES,'1');

// ******** SPECIAL USERS WHO CANNOT BE DELETED ******* 
// Users specified in this indice of the array by user id will not be able to be deleted or banned 
// from the system. Make sure the id(s) who you specify in this indice are for the correct user. 
// Multiple users can be listed here separated by a comma.
define(APPG_UNDELETEABLE,'1');

// ************** SUPER ADMINISTRATORS ****************
// List users intended to be granted super administrator access by user id. 
// Separate user id's  by comma.
define(APPG_ADMINS,'1');

/** ************* Mobile Options ***********************
    gcdpath is the directory where .gcd files are stored */
$cnfg['mobile']['gcdpath'] = '/home/starjam/public_html/d/'; // 
define(GCD_PATH,'/home/starjam/public_html/d/');

/** Mobile gateway url and integration information */
$cnfg['mobile']['gatewayurl_1'] = 'http://client.txtnation.com/ir_response.php';
define(IR_RESPONSE,'http://client.txtnation.com/ir_response.php');

// Database stuff
require_once(MAIN_DIR. 'includes/adodb/adodb.inc.php');
require_once(MAIN_DIR. 'includes/paso.php');
$conn = &ADONewConnection($TYPE);
$conn->PConnect($HOST, $USER, $PASS, $NAME);

$con = mysql_connect($HOST, $USER, $PASS) or die (mysql_error());
$db = mysql_select_db($NAME, $con) or die (mysql_error());
$mysql = mysql_select_db($NAME, $con) or die (mysql_error()); 

// Connected to the database class and creating a database object..
include(MAIN_DIR . "includes/config.inc.php");
include(MAIN_DIR . "class/database.class.php");

// create the $db ojbect
$db = new Database($config['server'], $config['user'], $config['pass'], $config['database'], $config['tablePrefix']);

// URL DIR for mobile downloads
$config["domain"] = "http://starjamz.com/d"; //no trailing slashes

// php graph library
include(MAIN_DIR . "class/phpgraphlib.php");
include(MAIN_DIR . "class/phpgraphlib_pie.php");

/**
 * create an upload object so we can use it anywhere on the site
 */
include(MAIN_DIR . "class/class.upload.php");
// create the $up object
//$up = new upload(); 

/**
 * Paths to Facebook Connect/Application files and data
 * FACEBOOK_APP_ID, FACEBOOK_SECRET defined
 */
define(FACEBOOK_API_KEY,'[Facebook API KEY GOES here]');
define(FACEBOOK_SECRET,'[Facebook Secret]');
define(FACEBOOK_APP_ID,[Facebook AP ID]);
define(FACEBOOK_XD_RECEIVER,MAIN_DIR.'fb/xd_receiver.htm');
define(FACEBOOK_OAUTH,MAIN_DIR.'fb/facebook.php'); // Facebook Classes
define(FACEBOOK_APP_DIR, '/home/starjam/fb'); // fix the problem the subdomain is not showing up.
define(FACEBOOK_API,'/home/starjam/fb/api');
 
/** Twitter Constants */
define(TWITTER_KEY,'[Twitter Key]');
define(TWITTER_SECRET,'[Twitter Secret]');
define(TWITTER_REQUEST_TOKEN,'http://twitter.com/oauth/request_token');
define(TWITTER_ACCESS_TOKEN,'http://twitter.com/oauth/access_token');
define(TWITTER_AUTHORIZE_URL,MAIN_DIR.'util/oauth/authorize');
define(TWITTER_CALLBACK,MAIN_DIR.'util/oauth/callback.php');
define(TWITTER_OAUTH,'/home/starjam/public_html/util/oauth'); //TwitterOauth Directory

define(TWITTER_JOIN,'just joined Starjamz - http://starjamz.com by logging in with Twitter #starjamz');
define(TWITTER_LIKE,'likes {title} at http://starjamz.com #starjamz');
define(TWITTER_GIFT,'has received {title} as a gift from @{actor} on http://starjamz.com {message}');
define(TWITTER_GIFT_PRV,'has received {title} as a gift from on http://starjamz.com {message}');





/** YAHOO! Constants */
define(YAHOO_KEY,'[Yahoo Key]');
define(YAHOO_SECRET,'[Yahoo Secret]');
define(YAHOO_APP_ID,'[Yahoo App ID]');
define(YAHOO_APP_DOMAIN,'starjamz.com');

/** reCAPTCHA Constants */
define(RCPUBLIC,'[reCAPTCHA public key]'); /// $publickey = "[reCAPTCHA public key]";
define(RCPRIVATE,'[reCAPTCHA private key]'); /// $privatekey = "[reCAPTCHA private key]";
