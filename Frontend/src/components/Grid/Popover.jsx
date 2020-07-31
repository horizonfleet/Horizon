import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Popover from "@material-ui/core/Popover";
import Typography from "@material-ui/core/Typography";
import Button from "@material-ui/core/Button";
import Slider from "./Slider";

const useStyles = makeStyles((theme) => ({
  typography: {
    padding: theme.spacing(5),
  },
}));

export default function SimplePopover(props) {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = React.useState(null);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  // const filterType = (filtername) => {
  //   if (filtername === "Delay") {
  //     return (
  // <Slider
  //   handleChange={props.handleChange}
  //   delay={props.state.delay}
  // ></Slider>
  //     );
  //   } else if (filtername === "Ecodriving") {
  //     return (
  //       <Ecofilter
  //         handleEcoChecks={props.handleEcoChecks}
  //         state={props.state}
  //       ></Ecofilter>
  //     );
  //   }
  // };

  const open = Boolean(anchorEl);
  const id = open ? "simple-popover" : undefined;

  function active() {
    if (props.state.delayFilter[0] === 0 && props.state.delayFilter[1] === 90) {
      return false;
    } else {
      return true;
    }
  }

  return (
    <React.Fragment>
      <Button
        aria-describedby={id}
        variant={active() ? "contained" : "outlined"}
        color="primary"
        onClick={handleClick}
        size="small"
      >
        Delay
      </Button>
      <Popover
        id={id}
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "center",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "center",
        }}
      >
        <Typography className={classes.typography}>
          <Slider
            handleDelayChange={props.handleDelayChange}
            delayFilter={props.state.delayFilter}
          ></Slider>
          {/* {filterType(props.name)} */}
          {/* <Slider
            handleChange={props.handleChange}
            delay={props.delay}
          ></Slider> */}
        </Typography>
      </Popover>
    </React.Fragment>
  );
}
