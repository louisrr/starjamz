<?php

if (!function_exists('curl_init')) {
  throw new Exception('Starjamz User Settings needs the CURL PHP extension.');
}
if (!function_exists('json_decode')) {
  throw new Exception('Starjamz User Settings needs the JSON PHP extension.');
}
class Settings {
	
	// array input
	public $array; 
	
	public $string;
	
	function __construct($array) {
		$this->$array =  $array;
		
		if ( isset ( $_REQUEST ) ) {
			$this->array = $_REQUEST;
		}
	}
	
	function idx($arr, $idx, $default=null) {
	  if ($idx === null || !is_array($arr)) {
	    return $default;
	  }
	  $idx = $idx >= 0 ? $idx : count($arr) + $idx;
	  return array_key_exists($idx, $arr) ? $arr[$idx] : $default;
	}
	
	function no_magic_quotes($val) {
	  if (get_magic_quotes_gpc()) {
	    return stripslashes($val);
	  } else {
	    return $val;
	  }
	}
	
	function parse_http_args($http_params, $keys) {
	  $result = array();
	  foreach ($keys as $key) {
	    $result[$key] = $this->no_magic_quotes($this->idx($http_params, $key));
	  }
	  return $result;
	}

}

?>
