<%@ Page Language="C#" %>

<%@ Import Namespace="System.Diagnostics" %>
<%@ Import Namespace="System.ServiceProcess" %>
<%@ Import Namespace="System.Data" %>
<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Globalization" %>
<script language="C#" runat="server">
    void Page_Load(Object sender, EventArgs e)
    {
        BindGrid(true);
    }

    void LogGrid_Change(Object sender, DataGridPageChangedEventArgs e)
    {
        // Set CurrentPageIndex to the page the user clicked.
        LogGrid.CurrentPageIndex = e.NewPageIndex;

        // Rebind the data.
        BindGrid(ButtonDetails.Text == "Show Details");
    }

    void BindGrid(bool filter_infos_and_warning)
    {
        EventLog aLog = new EventLog();
        aLog.Log = "Application";
        aLog.MachineName = ".";

        ArrayList al = new ArrayList();
        int maxEntries = 1000;
        if (aLog.Entries.Count > 0)
        {
            foreach (EventLogEntry ele in aLog.Entries)
            {
                if (ele.Source != "Libra")
                    continue;
                if (!filter_infos_and_warning || (ele.EntryType == EventLogEntryType.Information ||
                       ele.EntryType == EventLogEntryType.Error))
                    al.Add(ele);
            }
            al.Reverse();
            if (al.Count > maxEntries)
                al.RemoveRange(maxEntries, al.Count - maxEntries);
        }
        LogGrid.DataSource = al;
        LogGrid.DataBind();
    }

    void Timer1_Tick(object sender, EventArgs e)
    {
        BindGrid(ButtonDetails.Text == "Show Details");
    }

    ServiceController GetService(string serviceName)
    {
        ServiceController[] services = ServiceController.GetServices();
        foreach (var svc in services)
        {
            if (svc.ServiceName == serviceName)
                return svc;
        }
        return null;
    }

    void ButtonDetails_Click(Object sender, EventArgs e)
    {
        if (ButtonDetails.Text == "Show Details")
            ButtonDetails.Text = "Hide Details";
        else
            ButtonDetails.Text = "Show Details";
        BindGrid(ButtonDetails.Text == "Show Details");
    }

    void ButtonStop_Click(Object sender, EventArgs e)
    {
        ServiceController controller = GetService("Libra");
        if (controller == null)
            return;

        controller.Stop();
        controller.WaitForStatus(ServiceControllerStatus.Stopped);
    }

    void ButtonStart_Click(Object sender, EventArgs e)
    {
        ServiceController controller = GetService("Libra");
        if (controller == null)
            return;

        controller.Start();
        controller.WaitForStatus(ServiceControllerStatus.Running);
    }
</script>
<body bgcolor="#ffffff">
    <h3>Libra</h3>
    <form id="Form1" runat="server">
        <asp:Button ID="ButtonDetails"
            Text="Show Details"
            OnClick="ButtonDetails_Click"
            runat="server" />
        <asp:ScriptManager runat="server" ID="ScriptManager1">
        </asp:ScriptManager>
        <asp:UpdatePanel runat="server" ID="UpdatePanel1">
            <ContentTemplate>
                <asp:Timer runat="server" ID="Timer1" Interval="10000" OnTick="Timer1_Tick"></asp:Timer>
                <asp:DataGrid ID="LogGrid" runat="server"
                    AllowPaging="True"
                    PageSize="100"
                    PagerStyle-Mode="NumericPages"
                    PagerStyle-HorizontalAlign="Right"
                    PagerStyle-NextPageText="Next"
                    PagerStyle-PrevPageText="Prev"
                    OnPageIndexChanged="LogGrid_Change"
                    BorderColor="black"
                    BorderWidth="1"
                    GridLines="Both"
                    CellPadding="3"
                    CellSpacing="0"
                    Font-Name="Verdana"
                    Font-Size="8pt"
                    HeaderStyle-BackColor="#aaaadd"
                    AutoGenerateColumns="false">
                    <Columns>
                        <asp:BoundColumn HeaderText="Code" DataField="EntryType" />
                        <asp:BoundColumn HeaderText="Date/Time" DataField="TimeGenerated" />
                        <asp:BoundColumn HeaderText="Description" DataField="Message" />
                    </Columns>
                </asp:DataGrid>
                <asp:Button ID="ButtonStop"
                    Text="STOP"
                    OnClick="ButtonStop_Click"
                    runat="server" />
                <asp:Button ID="ButtonStart"
                    Text="START"
                    OnClick="ButtonStart_Click"
                    runat="server" />
            </ContentTemplate>
        </asp:UpdatePanel>
    </form>

</body>
</html>