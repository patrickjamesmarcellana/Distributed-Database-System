// change displayed input fields depending on selected database operation
$("#operation").change(() => {
    // determine selected database operation
    const selected_operation = $("#operation option:selected").attr("value")
    console.log("Selected Database Operation: " + selected_operation)

    // show only the ID input when operation is get or delete
    if(selected_operation === "get" || selected_operation === "delete") {
        $(".getAll-op").removeClass("hidden")
        $(".insert-delete-op").addClass("hidden")
    }

    // show no input field when operation is getAll
    if(selected_operation === "getAll") {
        $(".getAll-op").addClass("hidden")
    }

    // show all input fields when operation is insert or update
    if(selected_operation === "add" || selected_operation === "update") {
        $(".getAll-op").removeClass("hidden")
        $(".insert-delete-op").removeClass("hidden")
    }
})

// determine action after submitting form




