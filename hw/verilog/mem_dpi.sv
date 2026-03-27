import "DPI-C" context function int pmem_read(input int raddr);
import "DPI-C" context function void pmem_write(
  input int  waddr,
  input int  wdata,
  input byte wmask
);

module ram_dpi (
    input wire clk,
    input wire resetn,

    input wire arvalid,
    input wire [31:0] araddr,

    input wire awvalid,
    input wire [31:0] awaddr,

    input wire [31:0] wdata,
    input wire [ 7:0] wstrb,

    output wire [31:0] rdata
);

  always_ff @(posedge clk) begin
    if (resetn & awvalid) begin
      pmem_write(awaddr, wdata, wstrb);
    end
  end

  assign rdata = resetn & arvalid ? pmem_read(araddr) : 0;

endmodule


module rom_dpi (
    input wire clk,
    input wire resetn,
    input wire valid,
    input wire [31:0] raddr,
    output wire [31:0] rdata
);

  assign rdata = resetn & valid ? pmem_read(raddr) : 0;

endmodule
