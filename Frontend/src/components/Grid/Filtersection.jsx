import React from "react";
import Grid from "@material-ui/core/Grid";
import Popover from "./Popover";
import Button from "@material-ui/core/Button";

export default function SimpleSelect(props) {
  function active() {
    if (
      props.state.delayFilter[0] === 0 &&
      props.state.delayFilter[1] === 90
      // (parseInt(props.state.delay.delayArea[0]) ===
      //   parseInt(props.state.delay.minDelay) &&
      //   parseInt(props.state.delay.delayArea[1]) ===
      //     parseInt(props.state.delay.maxDelay)) === false
    ) {
      return false;
    } else {
      return true;
    }
  }

  return (
    <Grid container spacing={3} justify="flex-end">
      <Grid item>
        <Button
          color="secondary"
          size="small"
          disabled={active() ? false : true}
          onClick={props.handleClear}
        >
          x Clear filters
        </Button>
      </Grid>
      <Grid item>
        <Popover
          handleDelayChange={props.handleDelayChange}
          state={props.state}
          datasample={props.datasample}
        ></Popover>
      </Grid>
    </Grid>
  );
}
