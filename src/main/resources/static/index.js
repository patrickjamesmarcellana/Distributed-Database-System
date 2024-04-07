let currentOperation;

// change displayed input fields depending on selected database operation
$("#operation").change(() => {
    // determine selected database operation
    currentOperation = $("#operation option:selected").attr("value");
    console.log("Selected Database Operation: " + currentOperation);

    // show only the ID input when operation is get or delete
    if (currentOperation === "get" || currentOperation === "delete") {
        $(".getAll-op").removeClass("hidden");
        $(".insert-delete-op").addClass("hidden");
    }

    // show no input field when operation is getAll
    if (currentOperation === "getAll") {
        $(".getAll-op").addClass("hidden");
    }

    // show all input fields when operation is insert or update
    if (currentOperation === "add" || currentOperation === "update") {
        $(".getAll-op").removeClass("hidden");
        $(".insert-delete-op").removeClass("hidden");
    }

    $("#data-display").addClass("hidden");
});

$("#submit-btn").click(async (e) => {
    e.preventDefault();

    if (currentOperation === "add") {
        const data = formToObject();
        console.log("add operation, got:", data);
        const response = await fetch("/appointments/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        })
            .then((response) => {
                return response.text();
            })
            .catch((error) => console.error(error));
        console.log(response);
    }

    if (currentOperation === "delete") {
        const formData = formToObject();
        const apptId = formData["id"];
        console.log("delete operation, got:", apptId);
        const response = await fetch(`/appointments/delete/${apptId}`, {
            method: "DELETE",
        })
            .then((response) => {
                return response.text();
            })
            .catch((error) => console.error(error));
        console.log(response);
    }

    if (currentOperation === "getAll") {
      console.log("getAll operation");
      const response = await fetch("/appointments/getAll", {
          method: "GET",
          })
          .then(response => {
              if (!response.ok) {
                  throw new Error("Error fetching data");
              }
              return response.json();
          })
          .then(appointments => {
              console.log("Appointments received:", appointments);

              const tableContainer = $("<div>").addClass("px-4");
              const table = $("<table>").addClass("text-sm text-left rtl:text-right mt-1 overflow-x-auto overflow-y-auto h-screen");
              tableContainer.append(table);

              // Create the table header
              const thead = $("<thead>").addClass("text-xs text-black uppercase rounded border-b dark:text-black");
              const headerRow = $("<tr>");

              const headerLabels = ["Appointment ID", "Status", "Time Queued", "Queue Date", "Start Time", "End Time", "Appointment Type", "Virtual", "Patient Age", "Patient Gender", "Clinic/Hospital Name", "Clinic is Hospital", "Clinic City", "Clinic Province", "Clinic Region Name", "Doctor Main Specialty", "Doctor Age"];

              $.each(headerLabels, function(index, label) {
                  $("<th>").text(label).addClass("px-6 py-3").appendTo(headerRow);
              });

              headerRow.appendTo(thead);
              thead.appendTo(table);

              // Create the table body
              const tbody = $("<tbody>").attr("id", "appointments-list");

              // Populate table with appointments
              $.each(appointments, function(index, appointment) {
                  const row = $("<tr>").addClass("border-b hover:bg-gray-300");

                  const appointmentAttributes = ["id", "status", "timequeued", "queuedate", "starttime", "endtime", "appttype", "isvirtual", "px_age", "px_gender", "clinic_hospitalname", "clinic_ishospital", "clinic_city", "clinic_province", "clinic_regionname", "doctor_mainspecialty", "doctor_age"];

                  $.each(appointmentAttributes, function(index, attr) {
                      $("<td>").text(appointment[attr]).addClass("px-6 py-4").appendTo(row);
                  });

                  tbody.append(row);
              });

              table.append(tbody);

              // Append the table to a container in the document
              $("#data-display").empty().append(table);
              $("#data-display").removeClass("hidden");
          })
          .catch((error) => console.error(error));

    }
});

function formToObject() {
    return {
        id: $("#id").val(),
        status: $("#status").val(),
        timequeued: $("#timequeued").val(),
        queuedate: $("#queuedate").val(),
        starttime: $("#starttime").val(),
        endtime: $("#endtime").val(),
        appttype: $("#appttype").val(),
        isvirtual: $("#isvirtual").val(),
        px_age: $("#px_age").val(),
        px_gender: $("#px_gender").val(),
        clinic_hospitalname: $("#clinic_hospitalname").val(),
        clinic_ishospital: $("#clinic_ishospital").val(),
        clinic_city: $("#clinic_city").val(),
        clinic_province: $("#clinic_province").val(),
        clinic_regionname: $("#clinic_regionname").val(),
        doctor_mainspecialty: $("#doctor_mainspecialty").val(),
        doctor_age: $("#doctor_age").val(),
    };
}
