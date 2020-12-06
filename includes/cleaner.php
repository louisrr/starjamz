<?PHP

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
**********************************************************************************************************
*                                              Written by: Louis Robinson for BRXSTARJAM, inc.           *
**********************************************************************************************************/

class Cleaner {
		
	var $stopwords = array(" find ", " about ", " me ", " ever ", " each ");//you need to extend this big time.

	//this will remove punctuation
	var $symbols = array('/','\\','\'','"',',','.','<','>','?',';',':','[',']','{','}','|','=','+','-','_',')','(','*','&','^','%','$','#','@','!','~','`');

	function parseString($string) {
		$string = ' '.$string.' ';
		$string = $this->removeStopwords($string);
		$string = $this->removeSymbols($string);
		return $string;
	}
	
	function removeStopwords($string) {
		for ($i = 0; $i < sizeof($this->stopwords); $i++) {
			$string = str_replace($this->stopwords[$i],' ',$string);
		}
		
		//$string = str_replace('  ',' ',$string);
		return trim($string);
	}
	
	function removeSymbols($string) {
		for ($i = 0; $i < sizeof($this->symbols); $i++) {
			$string = str_replace($this->symbols[$i],' ',$string);
		}
			
		return trim($string);
	}
}

?>
