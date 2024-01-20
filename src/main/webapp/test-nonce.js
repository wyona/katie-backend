// INFO: Instructions how to test preventint XSS using a CSP nonce: Add this script to index.html (<script src="test-nonce.js"></script>), add QnA with answer <answer>Test XSS <b onmouseover="explode()">click me!</b></answer>, add/remove nonce from CSP and disable/enable safecontent pipe inside Angular component ask-question.component.html

/**
 *
 */
function explode() {
  alert('KABOOOOOOM!!!');
}
