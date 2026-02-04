function validate_Complete() {
{
    var c=document.getElementsByTagName("textarea")
    var comment;
    for (var k=0; k<c.length; k++)
    {
        if (c[k].name.indexOf("comments")!=-1)
        {
            comment=c[k];
            break;
        }
    }
    var vote;
    var a=document.getElementsByTagName("input");
    var voteField = new Array(3); //assumption, only three choices
    //get vote choices
    var counter=0;
    for (var i=0; i<a.length; i++)
    {
        if (a[i].name.indexOf("WfUserEvent0")!=-1)
        {
            voteField[counter++]=a[i];
        }
    }
    vote = getCheckedValue(voteField)
    if (vote =="Reject" || vote == "Amend")
        if (IsEmpty(comment))
        {
            alert("Please enter comments if you are choosing Amend or Reject.");
            return false;
        }
    return true;

}