/**
* INFO: Expand and collapse optional input fields
*/
function expandCollapse() {
    //alert("DEBUG: Expand/Collapse ...");

    var acc = document.getElementsByClassName("accordion")[0];
    acc.classList.toggle("active");
    var panel = acc.nextElementSibling;
    if (panel.style.maxHeight){
        panel.style.maxHeight = null;
    } else {
        panel.style.maxHeight = panel.scrollHeight + "px";
    }
}
