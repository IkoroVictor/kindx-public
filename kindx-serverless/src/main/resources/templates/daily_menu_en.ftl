<#assign menuDate = notification.menuDate?datetime>
Kitchen: *${notification.kitchenName}*
Posted: ${menuDate}
Menu: ${notification.url}

Today's menu contains::
<#list notification.foodItems as item>
    - _${item}_
</#list>



